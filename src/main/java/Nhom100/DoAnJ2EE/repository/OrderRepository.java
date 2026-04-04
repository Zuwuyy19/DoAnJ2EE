package Nhom100.DoAnJ2EE.repository;

import Nhom100.DoAnJ2EE.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    interface MonthlyRevenueSummary {
        Integer getYear();
        Integer getMonth();
        Double getRevenue();
    }

    interface WeeklyRevenueSummary {
        Integer getYear();
        Integer getWeek();
        Double getRevenue();
    }

    interface DailyRevenueSummary {
        String getDay();
        Double getRevenue();
    }

    interface YearlyRevenueSummary {
        Integer getYear();
        Double getRevenue();
    }

    interface TopUserSummary {
        Long getUserId();
        String getEmail();
        Long getTotalOrders();
        Double getRevenue();
    }

    @Query("select year(o.orderDate) as year, month(o.orderDate) as month, sum(o.totalAmount) as revenue " +
           "from Order o group by year(o.orderDate), month(o.orderDate) order by year(o.orderDate), month(o.orderDate)")
    List<MonthlyRevenueSummary> findMonthlyRevenue();

    @Query("select year(o.orderDate) as year, function('week', o.orderDate) as week, sum(o.totalAmount) as revenue " +
           "from Order o group by year(o.orderDate), function('week', o.orderDate) order by year(o.orderDate), function('week', o.orderDate)")
    List<WeeklyRevenueSummary> findWeeklyRevenue();

    @Query("select function('date_format', o.orderDate, '%Y-%m-%d') as day, sum(o.totalAmount) as revenue " +
           "from Order o group by function('date_format', o.orderDate, '%Y-%m-%d') order by function('date_format', o.orderDate, '%Y-%m-%d')")
    List<DailyRevenueSummary> findDailyRevenue();

    @Query("select year(o.orderDate) as year, month(o.orderDate) as month, sum(o.totalAmount) as revenue " +
           "from Order o where o.orderDate between :start and :end " +
           "group by year(o.orderDate), month(o.orderDate) order by year(o.orderDate), month(o.orderDate)")
    List<MonthlyRevenueSummary> findMonthlyRevenueBetween(@Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    @Query("select year(o.orderDate) as year, function('week', o.orderDate) as week, sum(o.totalAmount) as revenue " +
           "from Order o where o.orderDate between :start and :end " +
           "group by year(o.orderDate), function('week', o.orderDate) order by year(o.orderDate), function('week', o.orderDate)")
    List<WeeklyRevenueSummary> findWeeklyRevenueBetween(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("select function('date_format', o.orderDate, '%Y-%m-%d') as day, sum(o.totalAmount) as revenue " +
           "from Order o where o.orderDate between :start and :end " +
           "group by function('date_format', o.orderDate, '%Y-%m-%d') order by function('date_format', o.orderDate, '%Y-%m-%d')")
    List<DailyRevenueSummary> findDailyRevenueBetween(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query("select year(o.orderDate) as year, sum(o.totalAmount) as revenue " +
           "from Order o group by year(o.orderDate) order by year(o.orderDate)")
    List<YearlyRevenueSummary> findYearlyRevenue();

    @Query("select year(o.orderDate) as year, sum(o.totalAmount) as revenue " +
           "from Order o where o.orderDate between :start and :end " +
           "group by year(o.orderDate) order by year(o.orderDate)")
    List<YearlyRevenueSummary> findYearlyRevenueBetween(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("select o.user.id as userId, o.user.email as email, count(o.id) as totalOrders, sum(o.totalAmount) as revenue " +
           "from Order o group by o.user.id, o.user.email order by sum(o.totalAmount) desc")
    List<TopUserSummary> findTopUsers();
}
