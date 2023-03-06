package ed.inf.adbs.minibase.base;

import javax.management.relation.Relation;

public class ScanOperator {
    private final Relation baseRelation;

    public ScanOperator(Relation baseRelation) {
        this.baseRelation = baseRelation;
    }
}
