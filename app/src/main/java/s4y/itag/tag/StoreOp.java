package s4y.itag.tag;

public class StoreOp {
    final StoreOpType op;
    final TagInterface tag;

    public StoreOp(StoreOpType op, TagInterface tag) {
        this.op = op;
        this.tag = tag;
    }
}
