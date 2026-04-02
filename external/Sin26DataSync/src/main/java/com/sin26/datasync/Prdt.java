package com.sin26.datasync;

/**
 * 對應 MSSQL prdt 表：PRD_NO, NAME, SPC, IDX1, WH
 */
public class Prdt {
    private String prdNo;
    private String name;
    private String spc;
    private String idx1;
    private String wh;

    public Prdt() {}

    public Prdt(String prdNo, String name, String spc, String idx1, String wh) {
        this.prdNo = prdNo;
        this.name = name;
        this.spc = spc;
        this.idx1 = idx1;
        this.wh = wh;
    }

    public String getPrdNo() { return prdNo; }
    public void setPrdNo(String prdNo) { this.prdNo = prdNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpc() { return spc; }
    public void setSpc(String spc) { this.spc = spc; }
    public String getIdx1() { return idx1; }
    public void setIdx1(String idx1) { this.idx1 = idx1; }
    public String getWh() { return wh; }
    public void setWh(String wh) { this.wh = wh; }
}
