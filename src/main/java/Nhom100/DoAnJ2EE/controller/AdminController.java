package Nhom100.DoAnJ2EE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.Category;
import Nhom100.DoAnJ2EE.entity.Chapter;
import Nhom100.DoAnJ2EE.entity.Lesson;
import Nhom100.DoAnJ2EE.entity.OrderDetail;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.service.CourseService;
import Nhom100.DoAnJ2EE.service.UserService;
import Nhom100.DoAnJ2EE.repository.CategoryRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.repository.ChapterRepository;
import Nhom100.DoAnJ2EE.repository.LessonRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.RoleRepository;
import Nhom100.DoAnJ2EE.repository.OrderRepository;
import Nhom100.DoAnJ2EE.dto.PurchasedCourseSummary;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping
    public String adminDashboard(Model model,
            @RequestParam(value = "period", required = false, defaultValue = "month") String period,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "day", required = false) Integer day,
            @RequestParam(value = "singleDate", required = false) String singleDate,
            @RequestParam(value = "monthValue", required = false) String monthValue,
            @RequestParam(value = "yearValue", required = false) Integer yearValue) {
        model.addAttribute("courseCount", courseService.getAllCourses().size());
        model.addAttribute("categoryCount", categoryRepository.count());
        model.addAttribute("userCount", userRepository.count());
        if (singleDate != null && !singleDate.isBlank()) {
            LocalDate d = LocalDate.parse(singleDate);
            year = d.getYear();
            month = d.getMonthValue();
            day = d.getDayOfMonth();
        }
        if (monthValue != null && !monthValue.isBlank()) {
            java.time.YearMonth ym = java.time.YearMonth.parse(monthValue);
            year = ym.getYear();
            month = ym.getMonthValue();
        }
        if (yearValue != null) {
            year = yearValue;
        }
        RevenueData revenueData = buildRevenue(period, startDate, endDate, year, month, day);
        model.addAttribute("revenueLabels", revenueData.labels);
        model.addAttribute("revenueValues", revenueData.values);
        model.addAttribute("period", period);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("day", day);
        model.addAttribute("singleDate", singleDate);
        model.addAttribute("monthValue", monthValue);
        model.addAttribute("yearValue", yearValue);
        model.addAttribute("topCourses", limitTopCourses(orderDetailRepository.findTopCourses(), 5));
        model.addAttribute("topUsers", limitTopUsers(orderRepository.findTopUsers(), 5));
        return "admin/admin";
    }

    // --- QUẢN LÝ KHÓA HỌC ---

    @GetMapping("/courses")
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        return "admin/course/list";
    }

    @GetMapping("/courses/new")
    public String showAddForm(Model model) {
        model.addAttribute("course", new Course());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/course/form";
    }

    @GetMapping("/courses/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id).orElse(null);
        if (course == null)
            return "redirect:/admin/courses";
        model.addAttribute("course", course);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/course/form";
    }

    @GetMapping("/courses/{id}/content")
    public String showCourseContent(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id).orElse(null);
        if (course == null)
            return "redirect:/admin/courses";
        List<Chapter> chapters = chapterRepository.findByCourseIdOrderById(id);
        Map<Long, List<Lesson>> lessonsByChapter = new HashMap<>();
        for (Chapter chapter : chapters) {
            lessonsByChapter.put(chapter.getId(), lessonRepository.findByChapterIdOrderById(chapter.getId()));
        }
        model.addAttribute("course", course);
        model.addAttribute("chapters", chapters);
        model.addAttribute("lessonsByChapter", lessonsByChapter);
        return "admin/course/content";
    }

    @PostMapping("/courses/{id}/chapters")
    public String addChapter(@PathVariable Long id, @RequestParam String title) {
        Course course = courseService.getCourseById(id).orElse(null);
        if (course == null)
            return "redirect:/admin/courses";
        Chapter chapter = new Chapter();
        chapter.setTitle(title);
        chapter.setCourse(course);
        chapterRepository.save(chapter);
        return "redirect:/admin/courses/" + id + "/content";
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/update")
    public String updateChapter(@PathVariable Long courseId,
            @PathVariable Long chapterId,
            @RequestParam String title) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null)
            return "redirect:/admin/courses/" + courseId + "/content";
        chapter.setTitle(title);
        chapterRepository.save(chapter);
        return "redirect:/admin/courses/" + courseId + "/content";
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/lessons")
    public String addLesson(@PathVariable Long courseId,
            @PathVariable Long chapterId,
            @RequestParam(value = "lessonTitles", required = false) List<String> lessonTitles,
            @RequestParam(value = "lessonFiles", required = false) List<MultipartFile> lessonFiles) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null)
            return "redirect:/admin/courses/" + courseId + "/content";
        
        if (lessonTitles != null) {
            for (int i = 0; i < lessonTitles.size(); i++) {
                String title = lessonTitles.get(i);
                if (title == null || title.trim().isEmpty()) {
                    continue;
                }
                Lesson lesson = new Lesson();
                lesson.setTitle(title.trim());
                lesson.setChapter(chapter);
                
                // Optional: Handle file upload if lessonFiles is provided
                if (lessonFiles != null && i < lessonFiles.size()) {
                    MultipartFile file = lessonFiles.get(i);
                    if (file != null && !file.isEmpty()) {
                        String fileName = saveUploadedFile(file, "lessons");
                        if (fileName != null) {
                            lesson.setVideoUrl("/uploads/lessons/" + fileName);
                        }
                    }
                }
                
                lessonRepository.save(lesson);
            }
        }
        return "redirect:/admin/courses/" + courseId + "/content";
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/delete")
    public String deleteChapter(@PathVariable Long courseId, @PathVariable Long chapterId) {
        chapterRepository.deleteById(chapterId);
        return "redirect:/admin/courses/" + courseId + "/content";
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long courseId,
            @PathVariable Long chapterId,
            @PathVariable Long lessonId) {
        lessonRepository.deleteById(lessonId);
        return "redirect:/admin/courses/" + courseId + "/content";
    }

    @GetMapping("/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/edit")
    public String showEditLesson(@PathVariable Long courseId,
            @PathVariable Long chapterId,
            @PathVariable Long lessonId,
            Model model) {
        Course course = courseService.getCourseById(courseId).orElse(null);
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (course == null || chapter == null || lesson == null)
            return "redirect:/admin/courses/" + courseId + "/content";
        model.addAttribute("course", course);
        model.addAttribute("chapter", chapter);
        model.addAttribute("lesson", lesson);
        return "admin/course/lesson-edit";
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/update")
    public String updateLesson(@PathVariable Long courseId,
            @PathVariable Long chapterId,
            @PathVariable Long lessonId,
            @RequestParam String title,
            @RequestParam(required = false) MultipartFile lessonFile) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null)
            return "redirect:/admin/courses/" + courseId + "/content";
        lesson.setTitle(title);
        if (lessonFile != null && !lessonFile.isEmpty()) {
            String fileName = saveUploadedFile(lessonFile, "lessons");
            if (fileName != null) {
                lesson.setVideoUrl("/uploads/lessons/" + fileName);
            }
        }
        lessonRepository.save(lesson);
        return "redirect:/admin/courses/" + courseId + "/content";
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/content")
    public String updateChapterContent(@PathVariable Long courseId,
            @PathVariable Long chapterId,
            @RequestParam(required = false) MultipartFile chapterVideoFile,
            @RequestParam(required = false) MultipartFile chapterPptFile) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null)
            return "redirect:/admin/courses/" + courseId + "/content";

        String videoName = saveUploadedFile(chapterVideoFile, "chapters/videos");
        if (videoName == null && chapterVideoFile != null && !chapterVideoFile.isEmpty()) {
            return "redirect:/admin/courses/" + courseId + "/content";
        }
        if (videoName != null) {
            chapter.setContentVideoUrl("/uploads/chapters/videos/" + videoName);
        }

        String pptName = saveUploadedFile(chapterPptFile, "chapters/ppts");
        if (pptName == null && chapterPptFile != null && !chapterPptFile.isEmpty()) {
            return "redirect:/admin/courses/" + courseId + "/content";
        }
        if (pptName != null) {
            chapter.setContentPptUrl("/uploads/chapters/ppts/" + pptName);
        }

        chapterRepository.save(chapter);
        return "redirect:/admin/courses/" + courseId + "/content";
    }

    @PostMapping("/courses/save")
    public String saveCourse(@ModelAttribute("course") Course course,
            @RequestParam(required = false) MultipartFile courseVideoFile) {
        if (courseVideoFile != null && !courseVideoFile.isEmpty()) {
            String videoName = saveUploadedFile(courseVideoFile, "courses/videos");
            if (videoName != null) {
                course.setVideoUrl("/uploads/courses/videos/" + videoName);
            }
        }
        courseService.saveCourse(course);
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return "redirect:/admin/courses";
    }

    // --- QUAN LY KHOA HOC DA MUA ---

    @GetMapping("/purchased-courses")
    public String listPurchasedCourses(Model model) {
        List<Course> courses = courseService.getAllCourses();
        List<OrderDetail> details = orderDetailRepository.findAll();
        Map<Long, PurchasedCourseSummary> summaryMap = new LinkedHashMap<>();

        for (Course course : courses) {
            PurchasedCourseSummary summary = new PurchasedCourseSummary(
                    course.getId(),
                    course.getName(),
                    course.getTitle(),
                    course.getPrice());
            boolean hasDetail = course.getPurchasedDetail() != null
                    && !course.getPurchasedDetail().isEmpty();
            hasDetail = hasDetail
                    || (course.getPurchasedVideoUrl() != null && !course.getPurchasedVideoUrl().isEmpty())
                    || (course.getPurchasedDocUrl() != null && !course.getPurchasedDocUrl().isEmpty())
                    || (course.getPurchasedPptUrl() != null && !course.getPurchasedPptUrl().isEmpty())
                    || chapterRepository.existsByCourseId(course.getId());
            summary.setHasDetail(hasDetail);
            summaryMap.put(course.getId(), summary);
        }

        for (OrderDetail od : details) {
            if (od.getCourse() == null) {
                continue;
            }
            Long courseId = od.getCourse().getId();
            PurchasedCourseSummary summary = summaryMap.get(courseId);
            if (summary == null) {
                summary = new PurchasedCourseSummary(
                        courseId,
                        od.getCourse().getName(),
                        od.getCourse().getTitle(),
                        od.getCourse().getPrice());
                boolean hasDetail = od.getCourse().getPurchasedDetail() != null
                        && !od.getCourse().getPurchasedDetail().isEmpty();
                hasDetail = hasDetail
                        || (od.getCourse().getPurchasedVideoUrl() != null
                                && !od.getCourse().getPurchasedVideoUrl().isEmpty())
                        || (od.getCourse().getPurchasedDocUrl() != null
                                && !od.getCourse().getPurchasedDocUrl().isEmpty())
                        || (od.getCourse().getPurchasedPptUrl() != null
                                && !od.getCourse().getPurchasedPptUrl().isEmpty())
                        || chapterRepository.existsByCourseId(od.getCourse().getId());
                summary.setHasDetail(hasDetail);
                summaryMap.put(courseId, summary);
            }

            summary.incrementPurchaseCount();

            LocalDateTime orderDate = od.getOrder() != null ? od.getOrder().getOrderDate() : null;
            if (orderDate != null) {
                if (summary.getLastPurchaseDate() == null
                        || orderDate.isAfter(summary.getLastPurchaseDate())) {
                    summary.setLastPurchaseDate(orderDate);
                }
            }
        }

        model.addAttribute("purchasedCourses", new ArrayList<>(summaryMap.values()));
        return "admin/purchased/list";
    }

    @GetMapping("/purchased-courses/edit/{id}")
    public String showPurchasedCourseDetailForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id).orElse(null);
        if (course == null)
            return "redirect:/admin/purchased-courses";
        model.addAttribute("course", course);
        return "admin/purchased/form";
    }

    @PostMapping("/purchased-courses/save")
    public String savePurchasedCourseDetail(@RequestParam Long courseId,
            @RequestParam(required = false) String purchasedDetail,
            @RequestParam(required = false) MultipartFile purchasedVideoFile,
            @RequestParam(required = false) MultipartFile purchasedDocFile,
            @RequestParam(required = false) MultipartFile purchasedPptFile) {
        Course course = courseService.getCourseById(courseId).orElse(null);
        if (course == null)
            return "redirect:/admin/purchased-courses";
        course.setPurchasedDetail(purchasedDetail);
        String videoName = saveUploadedFile(purchasedVideoFile, "videos");
        if (videoName == null && purchasedVideoFile != null && !purchasedVideoFile.isEmpty()) {
            return "redirect:/admin/purchased-courses/edit/" + courseId;
        }
        if (videoName != null) {
            course.setPurchasedVideoUrl("/uploads/videos/" + videoName);
        }

        String docName = saveUploadedFile(purchasedDocFile, "docs");
        if (docName == null && purchasedDocFile != null && !purchasedDocFile.isEmpty()) {
            return "redirect:/admin/purchased-courses/edit/" + courseId;
        }
        if (docName != null) {
            course.setPurchasedDocUrl("/uploads/docs/" + docName);
        }

        String pptName = saveUploadedFile(purchasedPptFile, "ppts");
        if (pptName == null && purchasedPptFile != null && !purchasedPptFile.isEmpty()) {
            return "redirect:/admin/purchased-courses/edit/" + courseId;
        }
        if (pptName != null) {
            course.setPurchasedPptUrl("/uploads/ppts/" + pptName);
        }
        course.setPurchasedDetail(purchasedDetail);
        courseService.saveCourse(course);
        return "redirect:/admin/purchased-courses";
    }

    private String saveUploadedFile(MultipartFile file, String relativeDir) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetDir = baseDir.resolve(relativeDir);
            Files.createDirectories(targetDir);
            String fileName = buildStoredFileName(file);
            Path dest = targetDir.resolve(fileName);
            file.transferTo(dest.toFile());
            return fileName;
        } catch (IOException e) {
            return null;
        }
    }

    private String buildStoredFileName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + ext;
    }
    // --- QUẢN LÝ DANH MỤC ---

    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/category/list";
    }

    @GetMapping("/categories/new")
    public String showAddCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category/form";
    }

    @GetMapping("/categories/edit/{id}")
    public String showEditCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null)
            return "redirect:/admin/categories";
        model.addAttribute("category", category);
        return "admin/category/form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute("category") Category category) {
        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        // Lưu ý: Nếu danh mục có khóa học, hibernate sẽ báo lỗi hoặc xóa cascade tùy
        // config
        categoryRepository.deleteById(id);
        return "redirect:/admin/categories";
    }

    // --- QUẢN LÝ NGƯỜI DÙNG ---

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user/list";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id).orElse(null);
        if (user == null)
            return "redirect:/admin/users";
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/user/form";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute("user") User user) {
        // Lấy user cũ từ DB để giữ lại password (không thay đổi password ở đây)
        User existingUser = userService.getUserById(user.getId()).orElse(null);
        if (existingUser != null) {
            user.setPassword(existingUser.getPassword());
            user.setEmail(existingUser.getEmail()); // Giữ email không đổi nếu cần
            userService.saveUser(user);
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id) {
        User user = userService.getUserById(id).orElse(null);
        if (user != null) {
            user.setEnabled(!user.isEnabled());
            userService.saveUser(user);
        }
        return "redirect:/admin/users";
    }

    private List<OrderDetailRepository.TopCourseSummary> limitTopCourses(
            List<OrderDetailRepository.TopCourseSummary> list, int max) {
        if (list == null) return List.of();
        return list.size() <= max ? list : list.subList(0, max);
    }

    private List<OrderRepository.TopUserSummary> limitTopUsers(
            List<OrderRepository.TopUserSummary> list, int max) {
        if (list == null) return List.of();
        return list.size() <= max ? list : list.subList(0, max);
    }

    private static class RevenueData {
        List<String> labels;
        List<Double> values;
        RevenueData(List<String> labels, List<Double> values) {
            this.labels = labels;
            this.values = values;
        }
    }

    private RevenueData buildRevenue(String period, String startDate, String endDate,
            Integer year, Integer month, Integer day) {
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        if ("day".equalsIgnoreCase(period)) {
            List<OrderRepository.DailyRevenueSummary> rows;
            if (day != null && month != null && year != null) {
                LocalDateTime dStart = LocalDate.of(year, month, day).atStartOfDay();
                LocalDateTime dEnd = LocalDate.of(year, month, day).atTime(LocalTime.MAX);
                rows = orderRepository.findDailyRevenueBetween(dStart, dEnd);
            } else {
                rows = (start != null && end != null)
                        ? orderRepository.findDailyRevenueBetween(start, end)
                        : orderRepository.findDailyRevenue();
            }
            for (OrderRepository.DailyRevenueSummary d : rows) {
                labels.add(d.getDay());
                values.add(d.getRevenue() != null ? d.getRevenue() : 0.0);
            }
            return new RevenueData(labels, values);
        }
        if ("week".equalsIgnoreCase(period)) {
            List<OrderRepository.WeeklyRevenueSummary> rows = (start != null && end != null)
                    ? orderRepository.findWeeklyRevenueBetween(start, end)
                    : orderRepository.findWeeklyRevenue();
            for (OrderRepository.WeeklyRevenueSummary w : rows) {
                labels.add("Tuần " + w.getWeek() + "/" + w.getYear());
                values.add(w.getRevenue() != null ? w.getRevenue() : 0.0);
            }
            return new RevenueData(labels, values);
        }
        if ("month".equalsIgnoreCase(period) && year != null && month != null) {
            LocalDateTime mStart = LocalDate.of(year, month, 1).atStartOfDay();
            LocalDateTime mEnd = LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()).atTime(LocalTime.MAX);
            List<OrderRepository.MonthlyRevenueSummary> rows = orderRepository.findMonthlyRevenueBetween(mStart, mEnd);
            for (OrderRepository.MonthlyRevenueSummary m : rows) {
                if (m.getMonth() == null || m.getYear() == null) continue;
                String label = String.format("%02d/%d", m.getMonth(), m.getYear());
                labels.add(label);
                values.add(m.getRevenue() != null ? m.getRevenue() : 0.0);
            }
            return new RevenueData(labels, values);
        }
        if ("year".equalsIgnoreCase(period)) {
            List<OrderRepository.YearlyRevenueSummary> rows;
            if (year != null) {
                LocalDateTime yStart = LocalDate.of(year, 1, 1).atStartOfDay();
                LocalDateTime yEnd = LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);
                rows = orderRepository.findYearlyRevenueBetween(yStart, yEnd);
            } else {
                rows = (start != null && end != null)
                        ? orderRepository.findYearlyRevenueBetween(start, end)
                        : orderRepository.findYearlyRevenue();
            }
            for (OrderRepository.YearlyRevenueSummary y : rows) {
                labels.add(String.valueOf(y.getYear()));
                values.add(y.getRevenue() != null ? y.getRevenue() : 0.0);
            }
            return new RevenueData(labels, values);
        }
        List<OrderRepository.MonthlyRevenueSummary> rows = (start != null && end != null)
                ? orderRepository.findMonthlyRevenueBetween(start, end)
                : orderRepository.findMonthlyRevenue();
        for (OrderRepository.MonthlyRevenueSummary m : rows) {
            if (m.getMonth() == null || m.getYear() == null) continue;
            String label = String.format("%02d/%d", m.getMonth(), m.getYear());
            labels.add(label);
            values.add(m.getRevenue() != null ? m.getRevenue() : 0.0);
        }
        return new RevenueData(labels, values);
    }

    private LocalDateTime parseStart(String startDate) {
        if (startDate == null || startDate.isBlank()) return null;
        return LocalDate.parse(startDate).atStartOfDay();
    }

    private LocalDateTime parseEnd(String endDate) {
        if (endDate == null || endDate.isBlank()) return null;
        return LocalDate.parse(endDate).atTime(LocalTime.MAX);
    }
}
