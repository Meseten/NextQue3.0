package com.nextque.model;

import java.util.Set;
import java.util.HashSet;

public class Agent {
    private String agentId;
    private String agentName;
    private Set<ServiceType> skills;
    private boolean isAvailable;

    public Agent(String agentId, String agentName) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.isAvailable = true;
        this.skills = new HashSet<>();
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public Set<ServiceType> getSkills() {
        return skills;
    }

    public void addSkill(ServiceType skill) {
        this.skills.add(skill);
    }

    public boolean canHandle(ServiceType serviceType) {
        return skills.contains(serviceType);
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    @Override
    public String toString() {
        return agentName + " (" + agentId + ")";
    }
}
