package com.github.dmtk;

class Command {
    private final String prodNum;
    private final String quantity;
    private final String sequence;
    public Command(String prodNum,String quantity,String sequence) {
        this.prodNum = prodNum;
        this.quantity = quantity;
        this.sequence = sequence;
    }
    
    public String getProdNum() {
        return prodNum;
    }
    
    public String getQuantity() {
        return quantity;
    }
    
    public String getSequence() {
        return sequence;
    }
}