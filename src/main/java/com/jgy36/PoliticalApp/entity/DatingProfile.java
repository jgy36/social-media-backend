package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "dating_profile")
public class DatingProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private Integer age;

    private String location;

    // NEW FIELD: Height
    private String height;

    // NEW FIELD: Religion
    private String religion;

    // NEW FIELD: Relationship Type
    @Column(name = "relationship_type")
    private String relationshipType;

    // Add this field with the other NEW FIELDS:
    private String job;

    // Add this getter/setter:
    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    // NEW FIELD: Lifestyle/Dating Style
    private String lifestyle;

    @ElementCollection
    @CollectionTable(name = "dating_profile_photos", joinColumns = @JoinColumn(name = "dating_profile_id"))
    @Column(name = "photo_url")
    private List<String> photos;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "gender_preference")
    private String genderPreference;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "max_distance")
    private Integer maxDistance;

    // NEW FIELD: Prompts (store as JSON strings)
    @ElementCollection
    @CollectionTable(name = "dating_profile_prompts", joinColumns = @JoinColumn(name = "dating_profile_id"))
    @Column(name = "prompt_data", columnDefinition = "TEXT")
    private List<String> prompts;

    // Add these NEW FIELDS after the existing prompts field:

    // NEW FIELDS: Vitals & Vices
    @Column(name = "has_children")
    private String hasChildren;

    @Column(name = "want_children")
    private String wantChildren;

    private String drinking;

    private String smoking;

    private String drugs;

    @Column(name = "looking_for", columnDefinition = "TEXT")
    private String lookingFor;

    // NEW FIELD: Interests (store as JSON strings like prompts)
    @ElementCollection
    @CollectionTable(name = "dating_profile_interests", joinColumns = @JoinColumn(name = "dating_profile_id"))
    @Column(name = "interest_data", columnDefinition = "TEXT")
    private List<String> interests;

    // NEW FIELD: Virtues (store as JSON strings like prompts)
    @ElementCollection
    @CollectionTable(name = "dating_profile_virtues", joinColumns = @JoinColumn(name = "dating_profile_id"))
    @Column(name = "virtue_data", columnDefinition = "TEXT")
    private List<String> virtues;

// Add these NEW GETTERS AND SETTERS at the end of your existing getters/setters:

    public String getHasChildren() { return hasChildren; }
    public void setHasChildren(String hasChildren) { this.hasChildren = hasChildren; }

    public String getWantChildren() { return wantChildren; }
    public void setWantChildren(String wantChildren) { this.wantChildren = wantChildren; }

    public String getDrinking() { return drinking; }
    public void setDrinking(String drinking) { this.drinking = drinking; }

    public String getSmoking() { return smoking; }
    public void setSmoking(String smoking) { this.smoking = smoking; }

    public String getDrugs() { return drugs; }
    public void setDrugs(String drugs) { this.drugs = drugs; }

    public String getLookingFor() { return lookingFor; }
    public void setLookingFor(String lookingFor) { this.lookingFor = lookingFor; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public List<String> getVirtues() { return virtues; }
    public void setVirtues(List<String> virtues) { this.virtues = virtues; }

    // Default constructor
    public DatingProfile() {}

    // Existing getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getGenderPreference() { return genderPreference; }
    public void setGenderPreference(String genderPreference) { this.genderPreference = genderPreference; }

    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }

    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    public Integer getMaxDistance() { return maxDistance; }
    public void setMaxDistance(Integer maxDistance) { this.maxDistance = maxDistance; }

    // NEW GETTERS AND SETTERS
    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public String getLifestyle() { return lifestyle; }
    public void setLifestyle(String lifestyle) { this.lifestyle = lifestyle; }

    public List<String> getPrompts() { return prompts; }
    public void setPrompts(List<String> prompts) { this.prompts = prompts; }
}
