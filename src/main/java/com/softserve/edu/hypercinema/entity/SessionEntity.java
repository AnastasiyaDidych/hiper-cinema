package com.softserve.edu.hypercinema.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sun.security.krb5.internal.Ticket;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "session")
public class SessionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "id")
    private Long id;

    private LocalDate date;

    @Column (name = "start_time" )
    private LocalTime startTime;

    @Column (name = "end_time")
    private LocalTime endTime;

    @OneToMany(mappedBy = "session")
    private List<TicketEntity> tickets;

    @OneToOne
    @JoinColumn(name = "movie_id")
    private MovieEntity movie;


    @OneToOne
    @JoinColumn(name = "hall_id")
    private HallEntity hall;

//    @ManyToOne(cascade = {CascadeType.DETACH,CascadeType.MERGE,CascadeType.PERSIST,CascadeType.REFRESH})
//    @JoinColumn(name = "schedule_id")
//    private ScheduleEntity schedule;
}
