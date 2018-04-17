package com.softserve.edu.hypercinema.entity;


import com.softserve.edu.hypercinema.model.DaysModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "schedule")
public class ScheduleEntity extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "id")
    private Long id;

    @Column(name = "time")

    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(name = "day")
    private DaysModel daysModel;

    @OneToMany(mappedBy = "schedule",
            cascade = CascadeType.ALL)
    List<SessionEntity> sessions;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
    private List<MovieEntity> movies;

}

