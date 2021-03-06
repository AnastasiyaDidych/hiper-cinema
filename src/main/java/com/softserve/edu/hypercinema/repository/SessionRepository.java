package com.softserve.edu.hypercinema.repository;

import com.softserve.edu.hypercinema.entity.MovieEntity;
import com.softserve.edu.hypercinema.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity,Long> {

    List<SessionEntity> findAllByDate(LocalDate localDate);

    List<SessionEntity> findAllByActiveAndDate(Boolean aBoolean,LocalDate localDate);

    @Query(value = "SELECT distinct s.movie FROM SessionEntity s where s.active=true")
    List<MovieEntity> getDisMovies();

    @Query(value = "SELECT distinct s.date FROM SessionEntity s where movie_id=?1 and s.active=true")
    List<LocalDate> getDisDates(Long id);

    @Query("SELECT distinct s FROM SessionEntity s where movie_id=?1 and s.date=?2 and s.active=true")
    List<SessionEntity> getDisStatrtTimes(Long id,LocalDate localDate);

    List<SessionEntity> findAllByActive(boolean b);
}
