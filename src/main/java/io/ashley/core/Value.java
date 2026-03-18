package io.ashley.core;

import io.ashley.rule.HandleValueException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class Value {
   Object v = null;

   public static Value valueOf(Object v) {
      Value value = new Value();
      value.v = v;
      return value;
   }

   public void checkAndLoadValue() throws HandleValueException {
      if (this.v instanceof ValueLoader) {
         this.v = ((ValueLoader) this.v).loadValue();
      }
   }

   public int getInt() throws HandleValueException {
      this.checkAndLoadValue();
      if (v != null)
         return Integer.parseInt(v.toString());
      return 0;
   }

   public long getLong() throws HandleValueException {
      this.checkAndLoadValue();
      if (v != null)
         return Long.parseLong(v.toString());
      return 0;
   }

   public double getDouble() throws HandleValueException {
      this.checkAndLoadValue();
      if (v != null && !v.equals(""))
         return Double.parseDouble(v.toString());
      return 0;
   }

   public String getString() throws HandleValueException {
      this.checkAndLoadValue();
      if (this.v == null)
         return "";
      if (this.v instanceof BigDecimal) {
         return ((BigDecimal) this.v).stripTrailingZeros().toPlainString();
      } else {
         return v.toString();
      }
   }

   public List<String> getList() throws HandleValueException {
      this.checkAndLoadValue();
      List<String> list = new ArrayList<>();
      if (this.v instanceof List) {
         ((List<?>) this.v).forEach(f -> list.add(f.toString()));
      } else if (this.v instanceof String) {
         String[] lst = v.toString().replace(" ", "").replace("\t", "").split(",");
         for (String s : lst) {
            list.add(s);
         }
      }
      return list;
   }
}
