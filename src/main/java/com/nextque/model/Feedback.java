// Filename: Feedback.java
package com.nextque.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Feedback {
    private int id; // From database
    private String ticketNumber;
    private int rating; // e.g., 1-5
    private String comments;
    private LocalDateTime submissionTime;

    // Constructor for new feedback
    public Feedback(String ticketNumber, int rating, String comments) {
        this.ticketNumber = ticketNumber;
        this.rating = rating;
        this.comments = comments;
        this.submissionTime = LocalDateTime.now();
    }

    // Constructor for loading from DB
    public Feedback(int id, String ticketNumber, int rating, String comments, LocalDateTime submissionTime) {
        this.id = id;
        this.ticketNumber = ticketNumber;
        this.rating = rating;
        this.comments = comments;
        this.submissionTime = submissionTime;
    }
    
    // Getters
    public int getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public int getRating() { return rating; }
    public String getComments() { return comments; }
    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public String getFormattedSubmissionTime() {
        return submissionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        return "Feedback{" +
               "id=" + id +
               ", ticketNumber='" + ticketNumber + '\'' +
               ", rating=" + rating +
               ", comments='" + comments + '\'' +
               ", submissionTime=" + getFormattedSubmissionTime() +
               '}';
    }
}
