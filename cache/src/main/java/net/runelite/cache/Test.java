package net.runelite.cache;

public class Test {
	public static void main(String[] args) {
		int original32 = 135004182;
		int high16 = original32 >>> 16;
		int low16 = original32 & 0xFFFF;
		System.out.println(high16 + " - " + low16);
		
		
//		int[] int32s = new int[]{13566163, 13566164, 13566165, 13566166, 13566167, 13566168, 13566167, 13566166, 13566165, 13566164};
//		for(int int32 : int32s) {
//			int high16 = int32 >>> 16;
//			int low16 = int32 & 0xFFFF;
//			System.out.println(high16 + " - " + low16);
//			System.out.println(high16 << 16 | low16);
//		}
		
		
		int[] faces = new int[]{3, 4, 5, 2, 6, 1};
		int cake = 0;
		for(int i = 0; i < faces.length; i++) {
			cake += faces[i] << i;
		}
		System.out.println(cake);
		
	}
}
