package lanceur;

import lanceur.*;

public class MultiLanceur {
	public static void main(String[] args) {
		LanceArene.main(args);
		LanceIHM.main(args);

		for(int i = 0; i < 2 ; i++){
			LancePersonnage.main(args);
		}
	}
}
