package io.ashley.core;

import io.ashley.rule.RuleApplyingException;

import java.util.List;

public class Operator {

    Value leftValue = null;
    Value rightValue = null;
    Comparator compare = null;

    public Operator(Value lv, Value rv, Comparator compare) {
        this.leftValue = lv;
        this.rightValue = rv;
        this.compare = compare;
    }

    public enum Comparator {
        EQUAL, NOTEQUAL, GREATER_THAN, LESS_THAN, CONTAINS, CONTAINS_ANY, NOTCONTAINS, NOTCONTAINS_ANY
    }

    public boolean assertOp() throws RuleApplyingException {
        if (this.compare == Comparator.EQUAL) {
            return this.leftValue.getString().equals(this.rightValue.getString());
        } else if (this.compare == Comparator.NOTEQUAL) {
            return !this.leftValue.getString().equals(this.rightValue.getString());
        } else if (this.compare == Comparator.GREATER_THAN) {
            return this.leftValue.getDouble() > this.rightValue.getDouble();
        } else if (this.compare == Comparator.LESS_THAN) {
            return this.leftValue.getDouble() < this.rightValue.getDouble();
        } else if (this.compare == Comparator.NOTCONTAINS) {
            List<String> v1 = this.rightValue.getList();
            List<String> v2 = this.leftValue.getList();
            return !v2.containsAll(v1);
        } else if (this.compare == Comparator.CONTAINS) {
            List<String> v1 = this.rightValue.getList();
            List<String> v2 = this.leftValue.getList();
            return v2.containsAll(v1);
        } else if (this.compare == Comparator.CONTAINS_ANY) {
            List<String> lst1 = this.leftValue.getList();
            List<String> lst2 = this.rightValue.getList();
            for (String v1 : lst1) {
                if (lst2.contains(v1)) {
                    return true;
                }
            }
            return false;
        } else if (this.compare == Comparator.NOTCONTAINS_ANY) {
            List<String> lst1 = this.leftValue.getList();
            List<String> lst2 = this.rightValue.getList();
            for (String v2 : lst2) {
                if (!lst1.contains(v2)) {
                    return true;
                }
            }
            return false;
        }

        throw new RuleApplyingException("Error occurred with invalid comparator");
    }

    public Value getLeftValue() {
        return leftValue;
    }

    public Value getRightValue() {
        return rightValue;
    }
}
