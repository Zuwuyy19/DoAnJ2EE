package Nhom100.DoAnJ2EE.dto;

import java.time.LocalDateTime;

public class PurchasedCourseSummary {
    private Long courseId;
    private String courseName;
    private String courseTitle;
    private Double price;
    private int purchaseCount;
    private LocalDateTime lastPurchaseDate;
    private boolean hasDetail;

    public PurchasedCourseSummary(Long courseId, String courseName, String courseTitle, Double price) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseTitle = courseTitle;
        this.price = price;
        this.purchaseCount = 0;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public Double getPrice() {
        return price;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public void incrementPurchaseCount() {
        this.purchaseCount++;
    }

    public LocalDateTime getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public void setLastPurchaseDate(LocalDateTime lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }

    public boolean isHasDetail() {
        return hasDetail;
    }

    public void setHasDetail(boolean hasDetail) {
        this.hasDetail = hasDetail;
    }
}
