package com.softserve.edu.hypercinema.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "seat")
@Getter
@Setter
@NoArgsConstructor
public class SeatEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "row")
    private Integer row;

    @Column(name = "number")
    private Integer number;

    @OneToMany(mappedBy = "seat")
    private List<TicketEntity> tickets;
  
    /*??????????????????????????????????????*/
    @ManyToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id")
    private HallEntity hall;


}