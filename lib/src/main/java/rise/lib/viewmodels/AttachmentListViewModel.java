package rise.lib.viewmodels;

import java.util.ArrayList;
import java.util.List;

public class AttachmentListViewModel {
	public String collection;
	public String folder;
	public List<String> files = new ArrayList<>();
	public List<Float> lats = new ArrayList<>();
	public List<Float> lngs = new ArrayList<>();
}
