package squirrel.util;

public class Pair<Left, Right> {

	private final Left left;
	private final Right right;

	public static <Left, Right> Pair<Left, Right> createPair(Left left,
			Right right) {
		return new Pair<Left, Right>(left, right);
	}

	public Pair(Left left, Right right) {
		this.left = left;
		this.right = right;
	}

	public Left getLeft() {
		return left;
	}

	public Right getRight() {
		return right;
	}
}