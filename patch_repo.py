import re
with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "r") as f:
    text = f.read()

target = """    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long = database.workoutTemplateDao().insert(template)

    suspend fun updateTemplate(template: WorkoutTemplateEntity) = database.workoutTemplateDao().update(template)

    suspend fun deleteTemplate(template: WorkoutTemplateEntity) = database.workoutTemplateDao().delete(template)"""

replacement = """    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long {
        val nextOrder = (database.workoutTemplateDao().getAll().maxOfOrNull { it.orderIndex } ?: -1) + 1
        return database.workoutTemplateDao().insert(template.copy(orderIndex = nextOrder))
    }

    suspend fun updateTemplate(template: WorkoutTemplateEntity) = database.workoutTemplateDao().update(template)

    suspend fun deleteTemplate(template: WorkoutTemplateEntity) = database.workoutTemplateDao().delete(template)
    
    suspend fun hasTemplateHistory(templateId: Long): Boolean = database.workoutSessionDao().hasHistory(templateId)
    
    suspend fun swapTemplates(id1: Long, id2: Long) {
        val dao = database.workoutTemplateDao()
        val t1 = dao.getById(id1) ?: return
        val t2 = dao.getById(id2) ?: return
        
        // Handle unique constraint by temporarily setting one to -1
        val order1 = t1.orderIndex
        val order2 = t2.orderIndex
        dao.update(t1.copy(orderIndex = -1))
        dao.update(t2.copy(orderIndex = order1))
        dao.update(t1.copy(orderIndex = order2))
    }
    
    suspend fun updateTemplateCategory(templateId: Long, category: String) {
        val dao = database.workoutTemplateDao()
        val t = dao.getById(templateId) ?: return
        dao.update(t.copy(category = category))
    }
    
    suspend fun updateTemplateRestDays(templateId: Long, restDays: Int) {
        val dao = database.workoutTemplateDao()
        val t = dao.getById(templateId) ?: return
        dao.update(t.copy(restDaysAfter = restDays))
    }"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "w") as f:
    f.write(text)
