package com.example.data

import com.example.data.dao.JimDao
import com.example.data.model.AssessmentEntity
import com.example.data.model.AssignmentEntity
import com.example.data.model.CourseEntity
import com.example.data.model.CourseNoteEntity
import com.example.data.model.DocumentAnnotationEntity
import com.example.data.model.ExamEntity
import com.example.data.model.LectureEntity
import com.example.data.model.LibraryDocumentEntity
import com.example.data.model.StudentProfileEntity
import com.example.data.model.TimetableSessionEntity

object SampleData {
    suspend fun populateIfEmpty(dao: JimDao) {
        if (dao.getCourseCount() > 0) return

        // 1. Profile
        val profile = StudentProfileEntity(
            id = 1,
            name = "أحمد محمد",
            titleRole = "طالب جامعي",
            universityId = "20240123",
            university = "جامعة سيئون",
            college = "كلية الحاسوب",
            major = "تقنية المعلومات",
            academicLevel = "الثاني",
            academicAdvisor = "د. محمد علي",
            currentGpa = 3.42,
            targetGpa = 3.75,
            maxGpaScale = 4.0,
            totalCredits = 132,
            completedCredits = 45,
            semesterCredits = 15,
            isDarkMode = true
        )
        dao.saveProfile(profile)

        // 2. Courses
        val csId = dao.insertCourse(
            CourseEntity(
                name = "هندسة البرمجيات",
                code = "CS301",
                instructor = "د. وليد الشمري",
                classroom = "H2",
                creditHours = 3,
                colorHex = "#2563EB",
                iconKey = "computer",
                description = "مفاهيم دورة حياة تطوير البرمجيات، النمذجة بـ UML، والمنهجيات الرشيقة Agile."
            )
        )
        val matId = dao.insertCourse(
            CourseEntity(
                name = "الرياضيات المتقطعة",
                code = "MAT201",
                instructor = "د. خالد السعدي",
                classroom = "H1",
                creditHours = 3,
                colorHex = "#10B981",
                iconKey = "math",
                description = "المنطق الرياضي، نظرية المجموعات، الرسوم البيانية، ونظريات التوافيق والتباديل."
            )
        )
        val dbId = dao.insertCourse(
            CourseEntity(
                name = "قواعد البيانات",
                code = "DBS302",
                instructor = "د. طارق المنصوري",
                classroom = "Lab6",
                creditHours = 3,
                colorHex = "#F59E0B",
                iconKey = "database",
                description = "تصميم قواعد البيانات العلاقية، لغة الاستعلام SQL، وتطبيع الجداول Normalization."
            )
        )
        val aiId = dao.insertCourse(
            CourseEntity(
                name = "الذكاء الاصطناعي",
                code = "AI301",
                instructor = "د. سمير باوزير",
                classroom = "H3",
                creditHours = 3,
                colorHex = "#8B5CF6",
                iconKey = "ai",
                description = "مقدمة في خوارزميات البحث، التعلم الآلي، والشبكات العصبية الاصطناعية."
            )
        )
        val engId = dao.insertCourse(
            CourseEntity(
                name = "لغة إنجليزية",
                code = "ENG101",
                instructor = "أ. مريم العيدروس",
                classroom = "C1",
                creditHours = 2,
                colorHex = "#EF4444",
                iconKey = "language",
                description = "مهارات القراءة والكتابة التقنية وإعداد العروض التقديمية الأكاديمية."
            )
        )

        // 3. Timetable Sessions (Sunday to Thursday)
        val sessions = listOf(
            // Sunday (1 = الأحد)
            TimetableSessionEntity(courseId = csId, dayOfWeek = 1, startTime = "08:00", endTime = "09:00", classroom = "H2", sessionType = "محاضرة"),
            TimetableSessionEntity(courseId = matId, dayOfWeek = 1, startTime = "09:15", endTime = "10:15", classroom = "H1", sessionType = "محاضرة"),
            TimetableSessionEntity(courseId = dbId, dayOfWeek = 1, startTime = "12:00", endTime = "12:30", classroom = "Lab6", sessionType = "معمل"),
            TimetableSessionEntity(courseId = aiId, dayOfWeek = 1, startTime = "01:00", endTime = "02:00", classroom = "H3", sessionType = "محاضرة"),
            TimetableSessionEntity(courseId = engId, dayOfWeek = 1, startTime = "03:00", endTime = "04:30", classroom = "C1", sessionType = "محاضرة"),

            // Monday (2 = الإثنين)
            TimetableSessionEntity(courseId = matId, dayOfWeek = 2, startTime = "09:00", endTime = "10:30", classroom = "H1", sessionType = "محاضرة"),
            TimetableSessionEntity(courseId = csId, dayOfWeek = 2, startTime = "11:00", endTime = "01:00", classroom = "Lab2", sessionType = "معمل"),

            // Tuesday (3 = الثلاثاء)
            TimetableSessionEntity(courseId = aiId, dayOfWeek = 3, startTime = "08:30", endTime = "10:00", classroom = "H3", sessionType = "محاضرة"),
            TimetableSessionEntity(courseId = dbId, dayOfWeek = 3, startTime = "10:30", endTime = "12:00", classroom = "H1", sessionType = "محاضرة"),

            // Wednesday (4 = الأربعاء)
            TimetableSessionEntity(courseId = aiId, dayOfWeek = 4, startTime = "09:00", endTime = "11:00", classroom = "Lab5", sessionType = "معمل"),
            TimetableSessionEntity(courseId = engId, dayOfWeek = 4, startTime = "11:30", endTime = "01:00", classroom = "C1", sessionType = "محاضرة"),

            // Thursday (5 = الخميس)
            TimetableSessionEntity(courseId = csId, dayOfWeek = 5, startTime = "09:00", endTime = "11:00", classroom = "H2", sessionType = "محاضرة"),
            TimetableSessionEntity(courseId = dbId, dayOfWeek = 5, startTime = "11:30", endTime = "01:30", classroom = "Lab6", sessionType = "معمل")
        )
        dao.insertSessions(sessions)

        // 4. Assessments for CS301 (Software Engineering)
        dao.insertAssessment(AssessmentEntity(courseId = csId, name = "النصفي الأول", earnedScore = 18.0, maxScore = 20.0, weightPercent = 20.0, date = "2025/03/10", notes = "فصول UML ومتطلبات النظام"))
        dao.insertAssessment(AssessmentEntity(courseId = csId, name = "المشروع", earnedScore = 14.0, maxScore = 15.0, weightPercent = 15.0, date = "2025/04/05", notes = "تصميم معمارية تطبيق الجوال"))
        dao.insertAssessment(AssessmentEntity(courseId = csId, name = "التقييم العملي", earnedScore = 10.0, maxScore = 10.0, weightPercent = 10.0, date = "2025/04/05", notes = "مختبر اختبارات الوحدات Unit Testing"))
        dao.insertAssessment(AssessmentEntity(courseId = csId, name = "المشاركة", earnedScore = 5.0, maxScore = 5.0, weightPercent = 5.0, date = "2025/04/01", notes = "حضور ونقاش في القاعة"))

        // Assessments for Other Courses
        dao.insertAssessment(AssessmentEntity(courseId = matId, name = "النصفي", earnedScore = 26.0, maxScore = 30.0, weightPercent = 30.0, date = "2025/03/15"))
        dao.insertAssessment(AssessmentEntity(courseId = dbId, name = "النصفي", earnedScore = 24.0, maxScore = 30.0, weightPercent = 30.0, date = "2025/03/18"))
        dao.insertAssessment(AssessmentEntity(courseId = aiId, name = "النصفي", earnedScore = 28.0, maxScore = 30.0, weightPercent = 30.0, date = "2025/03/20"))
        dao.insertAssessment(AssessmentEntity(courseId = engId, name = "النصفي", earnedScore = 25.0, maxScore = 30.0, weightPercent = 30.0, date = "2025/03/22"))

        // 5. Lectures for CS301
        dao.insertLecture(LectureEntity(courseId = csId, title = "مقدمة في هندسة البرمجيات ودورة الحياة", lectureNumber = 1, lectureDate = "2025/02/10", summary = "شرح الفرق بين البرمجة وهندسة النظم، نماذج Waterfall و Agile.", instructorNotes = "التركيز على فهم المراحل الأساسية.", personalNotes = "مراجعة مخطط الشلال.", isReviewed = true))
        dao.insertLecture(LectureEntity(courseId = csId, title = "هندسة المتطلبات ونمذجة حالات الاستخدام", lectureNumber = 2, lectureDate = "2025/02/17", summary = "جمع المتطلبات الوظيفية وغير الوظيفية ورسم Use Case Diagrams.", instructorNotes = "الواجب الأول مرتبط بهذه المحاضرة.", personalNotes = "تم حل التمارين.", isReviewed = true))
        dao.insertLecture(LectureEntity(courseId = csId, title = "النمذجة الشيئية ومخططات الفئات Class Diagrams", lectureNumber = 3, lectureDate = "2025/02/24", summary = "العلاقات بين الفئات (وراثة، تجميع، تركيب) وتطبيقها في الكود.", instructorNotes = "مهم جداً للاختبار النصفي.", personalNotes = "يحتاج إعادة مراجعة.", isReviewed = false))
        dao.insertLecture(LectureEntity(courseId = csId, title = "معماريات البرمجيات وأنماط التصميم Design Patterns", lectureNumber = 4, lectureDate = "2025/03/03", summary = "شرح أنماط Singleton, Factory, Observer وتطبيقها.", instructorNotes = "سؤال مؤكد في المشروع.", personalNotes = "تدوين أمثلة بلغة كوتلن.", isReviewed = false))

        // 6. Assignments
        dao.insertAssignment(AssignmentEntity(courseId = csId, title = "تسليم مشروع البرمجة", description = "إكمال النموذج الأولي وتقديم وثيقة تحليل المتطلبات SRS مع مخططات UML.", dueDate = "2025/04/15", priority = "عاجل", status = "IN_PROGRESS"))
        dao.insertAssignment(AssignmentEntity(courseId = dbId, title = "اختبار قواعد البيانات", description = "مراجعة استعلامات SQL المعقدة و Join والمفاتيح الأساسية والأجنبية.", dueDate = "2025/04/18", priority = "متوسط", status = "NOT_STARTED"))
        dao.insertAssignment(AssignmentEntity(courseId = matId, title = "حل تمارين الرسوم البيانية Graph Theory", description = "تمارين الفصل الخامس وإيجاد أقصر مسار باستخدام خوارزمية ديكسترا.", dueDate = "2025/04/22", priority = "منخفض", status = "NOT_STARTED"))

        // 7. Exams
        dao.insertExam(ExamEntity(courseId = dbId, title = "اختبار قواعد البيانات النصفي", examDate = "2025/04/18", examTime = "10:00 ص - 12:00 م", examType = "عملي", classroom = "Lab6", topics = "SQL DDL, DML, Subqueries, Stored Procedures", prepNotes = "مراجعة ملاحظات المعمل السابق"))
        dao.insertExam(ExamEntity(courseId = aiId, title = "اختبار الذكاء الاصطناعي الأكاديمي", examDate = "2025/04/25", examTime = "09:00 ص - 11:00 ص", examType = "نظري", classroom = "H3", topics = "A* Search, Minimax, Decision Trees", prepNotes = "التركيز على مسائل الحسابات اليدوية"))

        // 8. Notes
        dao.insertNote(CourseNoteEntity(title = "أفكار للمشروع", content = "تطوير تطبيق لإدارة المهام الجامعية يتميز بالعمل بدون إنترنت وواجهة عصرية بتصميم Jim الأكاديمي.", category = "مهم", isPinned = true))
        dao.insertNote(CourseNoteEntity(title = "قائمة المراجع المهمة", content = "كتاب Software Engineering by Ian Sommerville الطبعة العاشرة.\nمرجع Artificial Intelligence: A Modern Approach.", category = "عام", isPinned = true))
        dao.insertNote(CourseNoteEntity(title = "ملاحظات للامتحان", content = "مراجعة الفصل 4 و 5 في قواعد البيانات. التركيز على استعلامات التجميع Group By و Having.", category = "مراجعة", isPinned = true))
        dao.insertNote(CourseNoteEntity(title = "أهدافي الدراسية", content = "تحقيق معدل 3.75 أو أعلى هذا الفصل والحفاظ على التفوق الأكاديمي.", category = "عام", isPinned = true))
        dao.insertNote(CourseNoteEntity(title = "ملاحظات عامة", content = "التسجيل في ورشة الحوسبة السحابية يوم الأحد القادم.", category = "عام", isPinned = false))

        // 9. Library Documents
        val doc1 = dao.insertDocument(
            LibraryDocumentEntity(
                courseId = csId,
                title = "محاضرات هندسة البرمجيات",
                fileType = "PDF",
                fileSize = "2.4 MB",
                category = "الكل",
                lastReadPage = 3,
                totalPages = 12,
                isBookmarked = true,
                contentSnippet = """
1. مقدمة في هندسة البرمجيات
تعتبر هندسة البرمجيات فرعاً من فروع الهندسة يركز على كافة جوانب إنتاج البرمجيات، بدءاً من المراحل المبكرة لتحديد متطلبات النظام وحتى صيانة النظام وتشغيله.

2. المبادئ الجوهرية للجودة
- الاعتمادية والموثوقية (Reliability)
- الكفاءة واستغلال الموارد (Efficiency)
- قابلية الصيانة والتطوير (Maintainability)
- الأمان وحماية البيانات (Security)

3. نماذج العمليات البرمجية
تتنوع النماذج حسب طبيعة المشروع ومتطلبات العميل:
أ. نموذج الشلال (Waterfall Model): تسلسل خطي صارم بين المراحل.
ب. التطوير التكراري والتزايدي (Iterative & Incremental): بناء النظام على دفعات مع تجربة كل جزء.
ج. المنهجيات الرشيقة (Agile Methodologies): التركيز على المرونة وسرعة الاستجابة للمتغيرات.
                """.trimIndent()
            )
        )
        dao.insertDocument(
            LibraryDocumentEntity(
                courseId = matId,
                title = "docx. البرهان الرياضي",
                fileType = "WORD",
                fileSize = "1.2 MB",
                category = "Word",
                lastReadPage = 1,
                totalPages = 8,
                isBookmarked = false,
                contentSnippet = """
مفاهيم البراهين الرياضية وطرق إثبات النظريات:
- البرهان المباشر (Direct Proof): افتراض صحة الفرض واستنتاج النتيجة منطقياً.
- البرهان بالتناقض (Proof by Contradiction): افتراض نفي النتيجة والوصول إلى تناقض صريح.
- الاستقراء الرياضي (Mathematical Induction): إثبات الحالة الأساسية ثم خطوة الاستقراء.
                """.trimIndent()
            )
        )
        dao.insertDocument(
            LibraryDocumentEntity(
                courseId = csId,
                title = "pptx - برمجي",
                fileType = "PPTX",
                fileSize = "4.8 MB",
                category = "PowerPoint",
                lastReadPage = 5,
                totalPages = 18,
                isBookmarked = true,
                contentSnippet = """
عرض تقديمي: معمارية النظم الحديثة
الشريحة 1: العنوان وفريق العمل
الشريحة 2: المشكلة والحلول المقترحة
الشريحة 3: معمارية الطبقات المنفصلة Clean Architecture
الشريحة 4: مزايا الاعتماد على التخزين المحلي Offline-First
الشريحة 5: النتائج والتقييم النهائي
                """.trimIndent()
            )
        )
        val doc4 = dao.insertDocument(
            LibraryDocumentEntity(
                courseId = aiId,
                title = "مراجع_الذكاء_الاصطناعي",
                fileType = "PDF",
                fileSize = "3.4 MB",
                category = "PDF",
                lastReadPage = 3,
                totalPages = 12,
                isBookmarked = true,
                contentSnippet = """
مقدمة في الذكاء الاصطناعي

1. ما هو الذكاء الاصطناعي؟
المجال المتفرع من علوم الحاسوب الذي يهتم بإنشاء أنظمة وبرمجيات قادرة على محاكاة السلوك البشري الذكي، مثل التفكير والتعلم والاستنتاج الذاتي وحل المشكلات المعقدة.

2. تصنيفات الذكاء الاصطناعي
- الذكاء الاصطناعي الضيق (Narrow AI): أنظمة متخصصة في مهمة واحدة محددة بكفاءة عالية (مثل التعرف على الصور ومحركات التوصية).
- الذكاء الاصطناعي العام (AGI): قدرة افتراضية على فهم أو تعلم أي مهمة فكرية يمكن للإنسان القيام بها.

3. خوارزميات البحث الذكية
- البحث الأعمى (Uninformed Search): BFS و DFS.
- البحث الإرشادي (Heuristic Search): خوارزمية A* وبحث التسلق البسيط.
                """.trimIndent()
            )
        )
        dao.insertDocument(
            LibraryDocumentEntity(
                courseId = csId,
                title = "صور_المشروع",
                fileType = "IMAGE",
                fileSize = "2.1 MB",
                category = "الكل",
                lastReadPage = 1,
                totalPages = 4,
                isBookmarked = false,
                contentSnippet = "مخططات واجهات المستخدم ومخطط قاعدة البيانات التفاعلي لمشروع التخرج."
            )
        )

        // 10. Sample Annotations for Document 4 (as seen in screenshot reader)
        dao.insertAnnotation(
            DocumentAnnotationEntity(
                documentId = doc4,
                page = 3,
                type = "HIGHLIGHT",
                content = "المجال المتفرع من علوم الحاسوب الذي يهتم بإنشاء أنظمة وبرمجيات قادرة على محاكاة السلوك البشري الذكي",
                colorHex = "#FEF08A"
            )
        )
        dao.insertAnnotation(
            DocumentAnnotationEntity(
                documentId = doc4,
                page = 3,
                type = "NOTE",
                content = "ملاحظة: مفهوم الذكاء الاصطناعي واسع ويشمل التعلم الآلي والشبكات العصبية والأنظمة الخبيرة.",
                colorHex = "#38BDF8"
            )
        )
    }
}
