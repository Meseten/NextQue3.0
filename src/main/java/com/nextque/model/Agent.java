// Filename: Agent.java
package com.nextque.model;

import java.util.Set;

/**
 * Represents an agent/staff member.
 * Placeholder for features like skill-based routing.
 */
public class Agent {
    private String agentId;
    private String agentName;
    private Set<ServiceType> skills; // Services the agent can handle
    private boolean isAvailable;

    public Agent(String agentId, String agentName) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.isAvailable = true;
        this.skills = new java.util.HashSet<>();
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
