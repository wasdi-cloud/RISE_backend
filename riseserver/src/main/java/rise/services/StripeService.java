package rise.services;



import java.util.Date;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.InvoiceLineItemCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.model.checkout.Session.CustomerDetails;

import rise.lib.config.RiseConfig;
import rise.lib.config.WasdiConfig;
import rise.lib.utils.Utils;
import rise.lib.utils.http.HttpCallResponse;
import rise.lib.utils.http.HttpUtils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.StripePaymentDetail;

import java.util.HashMap;



import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;


public class StripeService {

	private static final String s_sSTRIPE_BASE_URL = "https://api.stripe.com/v1";

	public StripePaymentDetail retrieveStripePaymentDetail(String sCheckoutSessionId) {
		StripePaymentDetail oStripePaymentDetail = new StripePaymentDetail();

		try {
			Stripe.apiKey = RiseConfig.Current.stripe.apiKey;

			Session oSession = Session.retrieve(sCheckoutSessionId);

			oStripePaymentDetail.setClientReferenceId(oSession.getClientReferenceId());

			CustomerDetails oCustomerDetails = oSession.getCustomerDetails();
			String sCustomerName = oCustomerDetails.getName();
			String sCustomerEmail = oCustomerDetails.getEmail();

			String sPaymentIntentId = oSession.getPaymentIntent();
			PaymentIntent oPaymentIntent = PaymentIntent.retrieve(sPaymentIntentId);
			String sPaymentStatus = oPaymentIntent.getStatus();
			String sPaymentCurrency = oPaymentIntent.getCurrency();
			Long lPaymentAmountInCents = oPaymentIntent.getAmount();

			oStripePaymentDetail.setPaymentIntentId(sPaymentIntentId);
			oStripePaymentDetail.setCustomerName(sCustomerName);
			oStripePaymentDetail.setCustomerEmail(sCustomerEmail);
			oStripePaymentDetail.setPaymentStatus(sPaymentStatus);
			oStripePaymentDetail.setPaymentCurrency(sPaymentCurrency);
			oStripePaymentDetail.setPaymentAmountInCents(lPaymentAmountInCents);

			String sInvoiceId = oPaymentIntent.getInvoice();

			if (sInvoiceId != null) {
				Invoice oInvoice = Invoice.retrieve(sInvoiceId);

				if (oInvoice != null) {
					InvoiceLineItemCollection oInvoiceLineItemCollection = oInvoice.getLines();
					List<InvoiceLineItem> oInvoiceLineItemData = oInvoiceLineItemCollection.getData();
					InvoiceLineItem oInvoiceLineItem = oInvoiceLineItemData.get(0);
					String sProductDescription = oInvoiceLineItem.getDescription();
					Long lPaymentDateInSeconds = oInvoiceLineItem.getPeriod().getStart();
					Date oDate = new Date(lPaymentDateInSeconds * 1_000);

					String sInvoicePdfUrl = oInvoice.getInvoicePdf();

					oStripePaymentDetail.setInvoiceId(sInvoiceId);
					oStripePaymentDetail.setProductDescription(sProductDescription);
					oStripePaymentDetail.setPaymentDateInSeconds(lPaymentDateInSeconds);
					oStripePaymentDetail.setDate(oDate);
					oStripePaymentDetail.setInvoicePdfUrl(sInvoicePdfUrl);
				}
			}
		} catch (StripeException oEx) {
			RiseLog.debugLog("StripeService.confirmation: " + oEx);
		}

		return oStripePaymentDetail;
	}

	/**
	 * Create a product on Stripe representing an app being sold in the Wasdi marketplace. 
	 * Then, it creates on Stripe the price being set for the app and associates it with the corresponding product.
	 * @param sName name for the product, it will be displayed when a user will purchase the product in Stripe
	 * @param sProcessorId id of the processor
	 * @param fPrice price of the app
	 * @return a map with two fields "productId" and "priceId", representing the corresponding information on Stripe. It returns null in case of error.
	 */
	public Map<String, String> createProductAppWithOnDemandPrice(String sName, String sProcessorId, Float fPrice) {

		if (Utils.isNullOrEmpty(sName)) {
			RiseLog.warnLog(
					"StripeService.createProductAppWithOnDemandPrice: can't create product on Stripe. Produc name is null or empty");
			return null;
		}
		
		if (Utils.isNullOrEmpty(sProcessorId)) {
			RiseLog.warnLog("StripeService.createAppWithOnDemandPrice: processor id is null or empty");
			return null;
		}

		RiseLog.debugLog("StripeService.createProductAppWithOnDemandPrice: creating product on stripe of app name: "
				+ sName + ", with price: " + fPrice);

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "products?";

		// request parameters
		List<String> asParameters = new ArrayList<>();
		try {
			// product name
			asParameters.add("name=" + URLEncoder.encode(sName, StandardCharsets.UTF_8.toString()));

			// tax code - same of subscriptions (General - Electronically Supplied Services)
			asParameters.add("tax_code=" + URLEncoder.encode("txcd_10000000", StandardCharsets.UTF_8.toString()));
			
			// metadata
			asParameters.add("metadata[productType]=processor");
			asParameters.add("metadata[priceType]=ondemand");
			asParameters.add("metadata[processorId]=" + URLEncoder.encode(sProcessorId, StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException oEx) {
			RiseLog.errorLog(
					"StripeService.createProductAppWithOnDemandPrice: can't create product on Stripe. Error in encoding the request parameters",
					oEx);
			return null;
		}

		String sAllParams = String.join("&", asParameters);
		sUrl += sAllParams;

		// headers
		Map<String, String> asHeaders = getStripeAuthHeader();

		

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sProductId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sProductId)) {
					RiseLog.warnLog(
							"StripeService.createProductAppWithOnDemandPrice: product id not found in the Stripe response. An error might have occured");
					return null;
				}

				RiseLog.debugLog("StripeService.createProductAppWithOnDemandPrice: product created on Stripe with id: "
						+ sProductId);

				String sPriceId = createProductOnDemandPrice(sProductId, fPrice);

				if (Utils.isNullOrEmpty(sPriceId)) {
					RiseLog.warnLog(
							"StripeService.createAppProduct: error creating the price for product " + sProductId);
					// TODO: at this point, I probably need to remove the product from Stripe
					return null;
				}

				RiseLog.debugLog("StripeService.createProductAppWithOnDemandPrice: created price " + sPriceId
						+ " for product " + sProductId);
				
				Map<String, String> oResult = new HashMap<>();
				oResult.put("productId", sProductId);
				oResult.put("priceId", sPriceId);
				return oResult;
			} catch (Exception oEx) {
				RiseLog.errorLog(
						"StripeService.createProductAppWithOnDemandPrice: there was an error reading the json response ",
						oEx);
				return null;
			}
		} else {
			RiseLog.errorLog(
					"StripeService.createProductAppWithOnDemandPrice: Can't create product on Stripe. Error while sending the request to Stripe.");
			return null;
		}
	}

	/**
	 * Deactivate a product and the price associated with it in Stripe
	 * @param sProductId the Stripe product id
	 * @return the deactivated product id if the de-activation was successfull, null otherwise
	 */
	public String deactivateProduct(String sProductId) {

		if (Utils.isNullOrEmpty(sProductId)) {
			RiseLog.warnLog(
					"StripeService.deactivateProduct: product id is null or empty. Impossible to deactivate product on Stripe");
			return null;
		}

		// first we deactivate all the active on demand prices associated to the product
		List<String> asPrices = getActiveOnDemandPricesId(sProductId);

		if (asPrices != null && asPrices.size() > 0) {

			for (String sPriceId : asPrices) {
				String sDeactivatedPrice = deactivatePrice(sPriceId);

				if (Utils.isNullOrEmpty(sDeactivatedPrice))
					RiseLog.warnLog(
							"StripeService.deactivateProduct: there was an error deactivating price " + sPriceId);
				else
					RiseLog.debugLog("StripeService.deactivateProduct: price " + sPriceId + " was deactivated");
			}
		}

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "products/" + sProductId + "?active=false";

		Map<String, String> oMapHeaders = getStripeAuthHeader();

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", oMapHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJSONResponse = new JSONObject(oHttpResponse.getResponseBody());
				boolean bIsActive = oJSONResponse.optBoolean("active");
				String sResponseProductId = oJSONResponse.optString("id", null);

				if (!sResponseProductId.equals(sProductId)) {
					RiseLog.warnLog("StripeService.deactivateProduct: product id " + sResponseProductId
							+ " from Stripe does not match requested product id " + sProductId);
					return null;
				}

				if (bIsActive) {
					RiseLog.warnLog(
							"StripeService.deactivateProduct: product " + sProductId + " is still active on Stripe");
					return null;
				}
				return sProductId;

			} catch (Exception oEx) {
				RiseLog.errorLog("StripeService.deactivateProduct: error reading the response from Stripe", oEx);
				return null;
			}
		} else {
			RiseLog.warnLog("StripeService.deactivateProduct: received an error code from Stripe");
			return null;
		}

	}

	/**
	 * Get the list of active price ids associated to a product
	 * @param sProductId the Stripe product id
	 * @return a list of active price ids for the product. It returns null in case of error
	 */
	public List<String> getActiveOnDemandPricesId(String sProductId) {

		if (Utils.isNullOrEmpty(sProductId)) {
			RiseLog.warnLog("StripeService.getActiveOnDemandPriceId: product id is null or empty. ");
			return null;
		}

		RiseLog.debugLog(
				"StripeService.getActiveOnDemandPriceId: get active on demand price for product " + sProductId);

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "prices/search";

		String sQuery = "";

		try {
			sQuery = URLEncoder.encode(("product:\'" + sProductId + "\'"), StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oEx) {
			RiseLog.errorLog("StripeService.getActiveOnDemandPriceId: error in encoding the request parameters", oEx);
			return null;
		}

		sUrl += "?query=" + sQuery;

		Map<String, String> asStripeHeaders = getStripeAuthHeader();
		HttpCallResponse oHttpResponse = HttpUtils.httpGet(sUrl, asStripeHeaders);

		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {

				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				JSONArray aoProductPrices = oJsonResponseBody.optJSONArray("data");

				if (aoProductPrices == null) {
					RiseLog.warnLog("StripeService.getActiveOnDemandPriceId: prices array is null for product id "
							+ sProductId);
				}

				if (aoProductPrices.length() == 0) {
					RiseLog.warnLog("StripeService.getActiveOnDemandPriceId: prices array is empty for product id "
							+ sProductId);
				}

				List<String> asActivePricesIds = new ArrayList<>();

				for (int i = 0; i < aoProductPrices.length(); i++) {
					JSONObject oJsonPrice = aoProductPrices.getJSONObject(i);

					if (oJsonPrice.optBoolean("active", false)) {
						String sPriceId = oJsonPrice.optString("id", null);
						if (!Utils.isNullOrEmpty(sPriceId))
							asActivePricesIds.add(sPriceId);
					}
				}

				return asActivePricesIds;

			} catch (Exception oEx) {
				RiseLog.errorLog("StripeService.getActiveOnDemandPriceId: exception reading the Stripe response ",
						oEx);
				return null;
			}
		} else {
			RiseLog.errorLog("StripeService.getActiveOnDemandPriceId: error while sending the request to Stripe");
			return null;
		}
	}

	/**
	 * Creates a Stripe price
	 * @param sProductId the product id to which the price should be attached
	 * @param fPrice the price value
	 * @return the id of the created Stripe price
	 */
	public String createProductOnDemandPrice(String sProductId, Float fPrice) {

		if (Utils.isNullOrEmpty(sProductId)) {
			RiseLog.errorLog(
					"StripeService.createProductPrice: product id is null or empty. Impossible to add the price to the product.");
			return null;
		}

		if (fPrice < 0) {
			RiseLog.errorLog("StripeService.createProductPrice: price can not be a negativa value");
			return null;
		}

		List<String> asParameters = new ArrayList<>();

		int iPriceInCents = (int) (fPrice * 100);

		try {
			asParameters.add("currency=eur");
			asParameters.add("product=" + URLEncoder.encode(sProductId, StandardCharsets.UTF_8.toString()));
			asParameters.add("unit_amount="
					+ URLEncoder.encode(Integer.toString(iPriceInCents), StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException oEx) {
			RiseLog.errorLog(
					"StripeService.createProductPrice: can't create product on Stripe. Error in encoding the request parameters",
					oEx);
			return null;
		}

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "prices?";

		String sAllParams = String.join("&", asParameters);
		sUrl += sAllParams;

		Map<String, String> asHeaders = getStripeAuthHeader();

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sPriceId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sPriceId)) {
					RiseLog.warnLog("StripeService.createProductPrice: no price id found after creation on stripe");
					// TODO: in this case I need probably to remove the price from Stripe
					return null;
				}
				RiseLog.debugLog("StripeService.createProductPrice: created price in Stripe with id: " + sPriceId);

				// one last check that the price is associated to the correct product (it should
				// always be the case)
				String sProductIdFromPrice = oJsonResponseBody.optString("product", null);

				if (!sProductIdFromPrice.equals(sProductId)) {
					RiseLog.warnLog(
							"StripeService.createProductPrice: the product id of the price and the expected product id are not matching");
					return null;
				}

				return sPriceId;
			} catch (Exception oEx) {
				RiseLog.errorLog("StripeService.createProductPrice: there was an error reading the json response ",
						oEx);
				return null;
			}
		} else {
			RiseLog.errorLog(
					"StripeService.createProductPrice: Can't create product on Stripe. Error while sending the request to Stripe");
			return null;
		}
	}
	
	/**
	 * Deactivates a given Stripe price id, setting its "active" field to false. 
	 * Before de-activating the price, it checks that the product has only one active price at time. 
	 * @param sPriceId the Stripe price id
	 * @return the de-activated price id if the operation was successful, null otherwise
	 */
	public String deactivateOnDemandPrice(String sProductId) {
		if (Utils.isNullOrEmpty(sProductId)) {
			RiseLog.warnLog("StripeService.deactivateOnDemandPrice: product id is null or empty.");
			return null;
		}
		
		List<String> asOnDemandPrices = getActiveOnDemandPricesId(sProductId);
		
		if (asOnDemandPrices == null || asOnDemandPrices.size() == 0) {
			RiseLog.warnLog("StripeService.deactivateOnDemandPrice: no active prices found for product " + sProductId);
			return null;
		}
		
		if (asOnDemandPrices.size() > 1) {
			RiseLog.warnLog("StripeService.updateOnDemandPrice: more than one active prices found for product " + sProductId + ". Can not decide the one to deactivate");
			return null;
		}
		
		return deactivatePrice(asOnDemandPrices.get(0));
	}

	/**
	 * Deactivates a given Stripe price id, setting its "active" field to false
	 * @param sPriceId the Stripe price id
	 * @return the de-activated price id if the operation was successfull, null otherwise
	 */
	public String deactivatePrice(String sPriceId) {
		if (Utils.isNullOrEmpty(sPriceId)) {
			RiseLog.errorLog("StripeService.deactivateProductPrice: price id is null or empty. Impossible to deactivate the price");
			return null;
		}

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "prices/" + sPriceId + "?" + "active=false";

		Map<String, String> asHeaders = getStripeAuthHeader();

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sStripePriceId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sStripePriceId)) {
					RiseLog.warnLog("StripeService.deactivateProductPrice: no price id found after creation on stripe");
					// TODO: in this case I need probably to remove the price from Stripe
					return null;
				}

				if (!sPriceId.equals(sStripePriceId)) {
					// this case should never happen
					RiseLog.warnLog("StripeService.deactivateProductPrice: the required price id and the returned price id are different");
					return null;
				}

				RiseLog.debugLog("StripeService.deactivateProductPrice: price in Stripe with id: " + sStripePriceId + " has been deactivated");

				return sStripePriceId;

			} catch (Exception oEx) {
				RiseLog.errorLog("StripeService.deactivateProductPrice: there was an error reading the json response ",
						oEx);
				return null;
			}
		} else {
			RiseLog.errorLog("StripeService.deactivateProductPrice: Can't deactivate price. Error while sending the request to Stripe");
			return null;
		}

	}
	
	/**
	 * Updates the active price of a Stripe product to a new value. The old one will be archived.
	 * @param sProductId Stripe product id
	 * @param fNewPrice new price of the product
	 * @return the new price id
	 */
	public String updateOnDemandPrice(String sProductId, float fNewPrice) {
		
		if (Utils.isNullOrEmpty(sProductId)) {
			RiseLog.warnLog("StripeService.updateOnDemandPrice: product id is null or empty.");
			return null;
		}
		
		List<String> asOnDemandPrices = getActiveOnDemandPricesId(sProductId);
		
		if (asOnDemandPrices == null || asOnDemandPrices.size() == 0) {
			RiseLog.warnLog("StripeService.updateOnDemandPrice: no active prices found for product " + sProductId);
			return null;
		}
		
		if (asOnDemandPrices.size() > 1) {
			RiseLog.warnLog("StripeService.updateOnDemandPrice: more than one active prices found for product " + sProductId + ". Can not decide the one to update");
			return null;
		}
		
		return updateOnDemandPrice(sProductId, asOnDemandPrices.get(0), fNewPrice);
		
	}

	/**
	 * 
	 * @param sProductId
	 * @param sPriceId
	 * @param fNewPrice
	 * @return
	 */
	public String updateOnDemandPrice(String sProductId, String sPriceId, float fNewPrice) {
		// TODO: adjust this so that we retrieve directly here the priceId
		if (Utils.isNullOrEmpty(sProductId)) {
			RiseLog.warnLog("StripeService.updateAppPrice: product id is null or empty. Impossible to update app price");
			return null;
		}

		if (Utils.isNullOrEmpty(sPriceId)) {
			RiseLog.warnLog("StripeService.updateAppPrice: price id is null or empty. Impossible to update app price");
			return null;
		}

		if (fNewPrice < 0) {
			RiseLog.errorLog("StripeService.updateAppPrice: price can not be a negativa value");
			return null;
		}

		// to update a price, we need first to deactivate the current price, then create a new one
		String sOldPriceId = deactivatePrice(sPriceId);

		if (Utils.isNullOrEmpty(sOldPriceId)) {
			RiseLog.warnLog("StripeService.updateAppPrice: no old price retrieved. The product won't be updated with the new price");
			return null;
		}

		RiseLog.debugLog("StripeService.updateAppPrice: old price with id " + sPriceId + " has been deactivated for the product " + sProductId);

		String sNewPriceId = createProductOnDemandPrice(sProductId, fNewPrice);

		if (Utils.isNullOrEmpty(sNewPriceId)) {
			RiseLog.warnLog("StripeService.updateAppPrice: new price id is null or empty");
			return null;
		}

		RiseLog.debugLog("StripeService.updateAppPrice: new price with id " + sNewPriceId
				+ " has been created for the product " + sProductId);

		return sNewPriceId;
	}

	/**
	 * Returns the standard authentication header for Stripe
	 * @return the authentication header for Stripe, containing the authentication token
	 */
	public Map<String, String> getStripeAuthHeader() {
		Map<String, String> asHeaders = new HashMap<>();

		byte[] yAuthEncBytes = Base64.getEncoder().encode(RiseConfig.Current.stripe.apiKey.getBytes());
		String sAuthStringEnc = new String(yAuthEncBytes);

		asHeaders.put("Authorization", "Basic " + sAuthStringEnc);

		return asHeaders;
	}
	
	/**
	 * Utility method to print the list the products on Stripe
	 */
	public void listProducts() {
		String sUrl = s_sSTRIPE_BASE_URL + "/products";

		Map<String, String> headers = getStripeAuthHeader();

		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl, headers);

		if (oResponse.getResponseCode() >= 200 && oResponse.getResponseCode() < 300) {
			System.out.println(oResponse.getResponseBody());
		} else {
			System.out.println("error code: " + oResponse.getResponseCode());
		}

	}
	
	/**
	 * Creates the Stripe payment link for a given Stripe price and a given WASDI processor. The information about the processor id will be stored among the metadata
	 * of the processor.
	 * @param sPriceId the Stripe price id that should be associated with the payment link 
	 * @param sWasdiProcessorId the WASDI processor id for which the payment link is being created
	 * @return the id of the payment link on Stripe. Returns null in case of errors
	 */
	public String createPaymentLink(String sPriceId, String sWasdiProcessorId) {
		if (Utils.isNullOrEmpty(sPriceId)) {
			RiseLog.errorLog("StripeService.createPaymentLink: price id is null or empty. Impossible to create payment link");
			return null;
		}
				
		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "payment_links?";

		// request parameters
		List<String> asParameters = new ArrayList<>();
		try {

			asParameters.add("line_items[0][price]=" + URLEncoder.encode(sPriceId, StandardCharsets.UTF_8.toString()));
			asParameters.add("line_items[0][quantity]=" + URLEncoder.encode("1", StandardCharsets.UTF_8.toString()));
			
			asParameters.add("metadata[wasdi_processor_id]=" + sWasdiProcessorId);
						
			asParameters.add("after_completion[type]=redirect");
			String sRedirectionUrl = RiseConfig.Current.serverApiAddress;
			if (!sRedirectionUrl.endsWith("/"))
				sRedirectionUrl += "/";
			sRedirectionUrl += "processors/stripe/confirmation/{CHECKOUT_SESSION_ID}";
			
			asParameters.add("after_completion[redirect][url]=" + URLEncoder.encode(sRedirectionUrl, StandardCharsets.UTF_8.toString()));

		} catch (UnsupportedEncodingException oEx) {
			RiseLog.errorLog(
					"StripeService.createPaymentLink: can't create product on Stripe. Error in encoding the request parameters",
					oEx);
			return null;
		}
		
		String sAllParams = String.join("&", asParameters);
		sUrl += sAllParams;

		// headers
		Map<String, String> asHeaders = getStripeAuthHeader();
		asHeaders.put("Content-Type", "application/x-www-form-urlencoded");

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sPaymentLinkId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sPaymentLinkId)) {
					RiseLog.warnLog(
							"StripeService.createPaymentLink: payment link id not found in the Stripe response.");
					return null;
				}

				RiseLog.debugLog("StripeService.createPaymentLink: payment link create on Stripe with id: " + sPaymentLinkId);
				
				return sPaymentLinkId;
				
			} catch (Exception oEx) {
				RiseLog.errorLog(
						"StripeService.createProductAppWithOnDemandPrice: there was an error reading the json response ",
						oEx);
				return null;
			}
		} else {
			RiseLog.errorLog(
					"StripeService.createProductAppWithOnDemandPrice: Can't create product on Stripe. Error while sending the request to Stripe.");
			return null;
		}
	}
	
	/**
	 * Given a Stripe payment link id, returns the associated URL
	 * @param sPaymentLinkId the payment link id on Stripe
	 * @return returns the Stripe payment URL
	 */
	public String retrievePaymentLink(String sPaymentLinkId) {

		if (Utils.isNullOrEmpty(sPaymentLinkId)) {
			RiseLog.warnLog("StripeService.retrievePaymentLink: payment link id is null or empty");
			return null;
		}

		RiseLog.debugLog("StripeService.retrievePaymentLink: get payment link for payment link id " + sPaymentLinkId);

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "payment_links/" + sPaymentLinkId;


		Map<String, String> asStripeHeaders = getStripeAuthHeader();
		HttpCallResponse oHttpResponse = HttpUtils.httpGet(sUrl, asStripeHeaders);

		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {

				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sPaymentLink = oJsonResponseBody.optString("url");

				if (sPaymentLink == null) {
					RiseLog.warnLog("StripeService.retrievePaymentLink: payment link is null for id "+ sPaymentLink);
				}

				return sPaymentLink;

			} catch (Exception oEx) {
				RiseLog.errorLog("StripeService.getActiveOnDemandPriceId: exception reading the Stripe response ",
						oEx);
				return null;
			}
		} else {
			RiseLog.errorLog("StripeService.getActiveOnDemandPriceId: error while sending the request to Stripe");
			return null;
		}

	}

	
	/**
	 * Deactivate a payment link id on Stripe
	 * @param sPaymentLinkId the id of the payment link
	 * @return the payment link id itself, if the link has been correctly de-activated on Stripe, null otherwise
	 */
	public String deactivatePaymentLink(String sPaymentLinkId) {
		
		if (Utils.isNullOrEmpty(sPaymentLinkId)) {
			RiseLog.errorLog("StripeService.deactivatePaymentLink: payment link id is null or empty. Impossible to update");
			return null;
		}

		RiseLog.debugLog("StripeService.deactivatePaymentLink: deactivate payment link " + sPaymentLinkId);

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "payment_links/" + sPaymentLinkId + "?" + "active=false";

		Map<String, String> asStripeHeaders = getStripeAuthHeader();
		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asStripeHeaders);

		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sResponsePaymentLinkId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sResponsePaymentLinkId)) {
					RiseLog.warnLog("StripeService.deactivatePaymentLink: payment link id not found in the Stripe response.");
					return null;
				}
				
				if (!oJsonResponseBody.optBoolean("active")) {
					RiseLog.debugLog("StripeService.deactivatePaymentLink: payment link deactivated on Stripe with id: " + sPaymentLinkId);
					return sPaymentLinkId;
				}
				else {
					RiseLog.errorLog("StripeService.deactivatePaymentLink: payment link is still active on Stripe with id: " + sPaymentLinkId + " but it should not");
					return null;
				}
				

			} catch (Exception oEx) {
				RiseLog.errorLog("StripeService.deactivatePaymentLink: exception reading the Stripe response ",oEx);
				return null;
			}
		} 
		else {
			RiseLog.errorLog("StripeService.deactivatePaymentLink: error while sending the request to Stripe");
			return null;
		}
		
	}
	
	/**
	 * Given a Stripe payment intent id, it adds to its metadata the information about the app payment id stored in WASDI
	 * @param sPaymentIntentId the Stripe payment intent id
	 * @param sWasdiAppPaymentId the WASDI app payment id 
	 * @return
	 */
	public String updatePaymentIntentWithAppPaymentId(String sPaymentIntentId, String sWasdiAppPaymentId) {
		
		if (Utils.isNullOrEmpty(sPaymentIntentId) || Utils.isNullOrEmpty(sWasdiAppPaymentId)) {
			RiseLog.errorLog("StripeService.updatePaymentIntentWithAppPaymentId: payment intent id or WASDI app payment id is null or empty. Impossible to update");
			return null;
		}

		RiseLog.debugLog("StripeService.updatePaymentIntentWithAppPaymentId: updating payment intent id " + sPaymentIntentId);

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "payment_intents/" + sPaymentIntentId;
		
		
		// request parameters
		String sMetadata = "";
		try {
			sMetadata += "metadata[appPaymentId]=" + URLEncoder.encode(sWasdiAppPaymentId, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oEx) {
			RiseLog.errorLog(
					"StripeService.updatePaymentIntentWithAppPaymentId: Error in encoding the request parameters",
					oEx);
			return null;
		}
		
		sUrl += "?" + sMetadata;

		Map<String, String> asStripeHeaders = getStripeAuthHeader();
		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asStripeHeaders);

		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sStripePaymentIntentId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sStripePaymentIntentId)) {
					RiseLog.warnLog("StripeService.updatePaymentIntentWithAppPaymentId: payment intent id not found in the Stripe response.");
					return null;
				}
				
				if (!sPaymentIntentId.equals(sStripePaymentIntentId)) {
					RiseLog.debugLog("StripeService.updatePaymentIntentWithAppPaymentId: mismatch in the Stripe payment ids");
					return null;
				}

				return sStripePaymentIntentId;

			} catch (Exception oEx) {
				RiseLog.errorLog("StripeService.updatePaymentIntentWithAppPaymentId: exception reading the Stripe response ",oEx);
				return null;
			}
		} 
		else {
			RiseLog.errorLog("StripeService.updatePaymentIntentWithAppPaymentId: error while sending the request to Stripe");
			return null;
		}
		
		
	}
	

}
