package eu.arrowhead.common.dto.shared;

import java.io.Serializable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.gson.Gson;

import eu.arrowhead.common.CommonConstants;

public class SenML implements Serializable {

  //=================================================================================================
  // members
  private String bn;
  private Double bt;
  private String bu;
  private Double bv;
  private Double bs;
  private Short bver;
  private String n;
  private String u;
  private Double v;
  private String vs;
  private Boolean vb;
  private Double s;
  private Double t;
  private Double ut;

  //=================================================================================================
  // methods

  //-------------------------------------------------------------------------------------------------
  public SenML() {
  }

  public String toString() {
      Gson gson = new Gson();
      String json = gson.toJson(this); 
      return json;
  }

  public void setBn(String bn) {
    this.bn = bn;
  }

  public String getBn() {
    return bn;
  }

  public void setV(Double v) {
    this.v = v;
  }

  public Double getV() {
    return v;
  }

  public void setVb(Boolean vb) {
    this.vb = vb;
  }

  public Boolean getVb() {
    return vb;
  }

  public void setVs(String vs) {
    this.vs = vs;
  }

  public String getVs() {
    return vs;
  }

  public void setN(String n) {
    this.n = n;
  }

  public String getN() {
    return n;
  }

  public void setBt(Double bt) {
    this.bt = bt;
  }

  public Double getBt() {
    return bt;
  }

  public void setT(Double t) {
    this.t = t;
  }

  public Double getT() {
    return t;
  }

  public void setBu(String bu) {
    this.bu = bu;
  }

  public String getBu() {
    return bu;
  }

  //-------------------------------------------------------------------------------------------------
}
