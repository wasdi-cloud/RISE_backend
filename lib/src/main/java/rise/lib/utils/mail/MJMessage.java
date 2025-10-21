package rise.lib.utils.mail;

import java.util.ArrayList;

public class MJMessage {
	public MJRecipient From;
	public ArrayList<MJRecipient> To = new ArrayList<>();
	public String Subject;
	public String TextPart;
	public String HTMLPart;
	public ArrayList<MJRecipient> Cc=new ArrayList<>();
}
