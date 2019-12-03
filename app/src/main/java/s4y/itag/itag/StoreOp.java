package s4y.itag.itag;

public class StoreOp {
    final StoreOpType op;
    final ITagInterface tag;

    public StoreOp(StoreOpType op, ITagInterface tag) {
        this.op = op;
        this.tag = tag;
    }
}
