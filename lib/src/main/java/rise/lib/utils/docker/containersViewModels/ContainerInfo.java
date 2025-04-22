package rise.lib.utils.docker.containersViewModels;

import java.util.ArrayList;
import java.util.List;

public class ContainerInfo {
	public String Id;
	public String Image;
	public String ImageId;
	public String State;
	public String Status;
	public List<String> Names = new ArrayList<>(); 
}
