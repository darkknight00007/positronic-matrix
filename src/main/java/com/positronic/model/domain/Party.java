package com.positronic.model.domain;

public class Party {
    private String id;
    private String name;
    private String lei; // Legal Entity Identifier
    private String jurisdiction;
    
    public Party(String id, String name, String lei, String jurisdiction) {
        this.id = id;
        this.name = name;
        this.lei = lei;
        this.jurisdiction = jurisdiction;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getLei() { return lei; }
    public String getJurisdiction() { return jurisdiction; }
}

class Account {
    private String accountId;
    private Party owner;
    private String accountType; // NOSTRO, VOSTRO, CLIENT
    
    public Account(String accountId, Party owner, String accountType) {
        this.accountId = accountId;
        this.owner = owner;
        this.accountType = accountType;
    }
    
    public String getAccountId() { return accountId; }
    public Party getOwner() { return owner; }
    public String getAccountType() { return accountType; }
}
