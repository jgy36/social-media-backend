package com.jgy36.PoliticalApp.dto;

public class DatingFilters {
    private String location;
    private String education;
    private String lifestyle;
    private String religion;
    private String relationshipType;
    private String drinking;
    private String smoking;
    private String hasChildren;
    private String wantChildren;

    // Constructors
    public DatingFilters() {}

    // Getters and Setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getLifestyle() { return lifestyle; }
    public void setLifestyle(String lifestyle) { this.lifestyle = lifestyle; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public String getDrinking() { return drinking; }
    public void setDrinking(String drinking) { this.drinking = drinking; }

    public String getSmoking() { return smoking; }
    public void setSmoking(String smoking) { this.smoking = smoking; }

    public String getHasChildren() { return hasChildren; }
    public void setHasChildren(String hasChildren) { this.hasChildren = hasChildren; }

    public String getWantChildren() { return wantChildren; }
    public void setWantChildren(String wantChildren) { this.wantChildren = wantChildren; }

    public boolean hasFilters() {
        return education != null || lifestyle != null || religion != null ||
                relationshipType != null || drinking != null || smoking != null ||
                hasChildren != null || wantChildren != null;
    }
}
