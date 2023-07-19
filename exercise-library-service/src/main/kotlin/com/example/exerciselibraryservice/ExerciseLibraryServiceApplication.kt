package com.example.exerciselibraryservice

import jakarta.persistence.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

enum class RepRange(val range: Pair<Int, Int>) {
    ONE_TO_THREE(1 to 3),
    THREE_TO_SIX(3 to 6),
    FIVE_TO_TEN(5 to 10),
    TEN_TO_TWENTY(10 to 20),
    TEN_TO_FIFTEEN(10 to 15),
    TWENTY_TO_THIRTY(20 to 30)
}

enum class MuscleGroup {
    QUADS,
    HAMSTRINGS,
    CALVES,
    GLUTES,
    BACK,
    TRAPS,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    PECS,
    ABS,
    BICEP,
    TRICEP,
    FOREARMS
}

enum class TargetMuscleFactor(val factor: Double) {
    ONE(1.0),
    POINT_FIVE(0.5)
}

@Entity
class Exercise(
    var name: String,
    var notes: String = "",
    @ElementCollection var targetMuscles: MutableMap<MuscleGroup, TargetMuscleFactor> = mutableMapOf(),
    @ElementCollection var preferredRepRanges: MutableSet<RepRange> = mutableSetOf(),
    var deleted: Boolean = false,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = -1
)

interface ExerciseRepository : JpaRepository<Exercise, Long> {
    fun findAllByDeletedIsFalse(): List<Exercise>
}

data class ExerciseDto(
    val name: String,
    val notes: String = "",
    val targetMuscles: Map<MuscleGroup, TargetMuscleFactor> = emptyMap(),
    val preferredRepRanges: Set<RepRange> = emptySet(),
    var id: Long? = null,
)


interface ExerciseService {
    fun getAll(): List<ExerciseDto>
    fun getById(id: Long): ExerciseDto
    fun createExercise(exerciseDto: ExerciseDto): ExerciseDto
    fun updateExercise(id: Long, exerciseDto: ExerciseDto)
    fun deleteExercise(id: Long)
}

@Service
class ExerciseServiceImpl(private val exerciseRepository: ExerciseRepository) : ExerciseService {
    override fun getAll(): List<ExerciseDto> {
        return exerciseRepository.findAllByDeletedIsFalse()
            .map {
                ExerciseDto(
                    it.name,
                    it.notes,
                    it.targetMuscles,
                    it.preferredRepRanges,
                    it.id
                )
            }
    }

    override fun getById(id: Long): ExerciseDto {
        return exerciseRepository.findByIdOrNull(id)
            ?.let {
                ExerciseDto(
                    it.name,
                    it.notes,
                    it.targetMuscles,
                    it.preferredRepRanges,
                    it.id
                )
            } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @Transactional
    override fun createExercise(exerciseDto: ExerciseDto): ExerciseDto {
        return exerciseDto.apply {
            val exercise = Exercise(name, notes, targetMuscles.toMutableMap(), preferredRepRanges.toMutableSet())
            exerciseRepository.save(exercise)
            id = exercise.id
        }
    }

    @Transactional
    override fun updateExercise(id: Long, exerciseDto: ExerciseDto) {
        exerciseRepository.findByIdOrNull(id)
            ?.let {
                it.name = exerciseDto.name
                it.notes = exerciseDto.notes
                it.targetMuscles.clear()
                it.targetMuscles = exerciseDto.targetMuscles.toMutableMap()
                it.preferredRepRanges.clear()
                it.preferredRepRanges = exerciseDto.preferredRepRanges.toMutableSet()

                exerciseRepository.save(it)
            }
    }

    @Transactional
    override fun deleteExercise(id: Long) {
        exerciseRepository.findByIdOrNull(id)
            ?.let {
                it.deleted = true
                exerciseRepository.save(it)
            }
    }
}

@RestController
@RequestMapping("/api/v1/exercises")
class ExerciseController(private val exerciseService: ExerciseService) {
    @GetMapping
    fun getAll() = exerciseService.getAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = exerciseService.getById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createNew(@Validated @RequestBody exerciseDto: ExerciseDto) = exerciseService.createExercise(exerciseDto)

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun update(@PathVariable id: Long, @Validated @RequestBody exerciseDto: ExerciseDto) {
        exerciseService.updateExercise(id, exerciseDto)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        exerciseService.deleteExercise(id)
    }

}

@SpringBootApplication
class ExerciseLibraryServiceApplication

fun main(args: Array<String>) {
    runApplication<ExerciseLibraryServiceApplication>(*args)
}
