package com.softserve.edu.hypercinema.service.impl;

import com.softserve.edu.hypercinema.constants.CoefficientType;
import com.softserve.edu.hypercinema.constants.SeatType;
import com.softserve.edu.hypercinema.converter.ScheduleConverter;
import com.softserve.edu.hypercinema.dto.Schedule;
import com.softserve.edu.hypercinema.dto.SessionDto;
import com.softserve.edu.hypercinema.entity.*;
import com.softserve.edu.hypercinema.repository.SessionRepository;
import com.softserve.edu.hypercinema.service.HallService;
import com.softserve.edu.hypercinema.service.MovieService;
import com.softserve.edu.hypercinema.service.SessionService;
import com.softserve.edu.hypercinema.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
@Transactional
public class SessionServiceImpl  implements SessionService {

    private final MathContext mc = new MathContext(3);
    private final int priceScale = 0;

    private final String MOVIE_ALREADY_EXISTS_MESSAGE = "movie already exist";
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private HallService hallService;

    @Autowired
    private ScheduleConverter scheduleConverter;



    @Override
    public SessionEntity getSession(Long id) {
        return sessionRepository.findById(id).orElse(null);
    }

    @Override
    public void createSession(SessionEntity sessionEntity) {
        sessionRepository.save(sessionEntity);
    }

    @Override
    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);
    }

    @Override
    public void updateSession(SessionEntity sessionEntity) {
        sessionRepository.save(sessionEntity);
    }

    @Override
    public List<SessionEntity> getSessions() {
        return sessionRepository.findAll();
    }

    @Override
    public void generateSession(SessionDto sessionDto) {

        SessionEntity sessionEntity = new SessionEntity();
        MovieEntity movieEntity = movieService.getMovie(sessionDto.getMovieId());
        sessionEntity.setMovie(movieEntity);
        sessionEntity.setHall(hallService.getHall(sessionDto.getHallId()));
        sessionEntity.setDate(LocalDate.parse(sessionDto.getDate(), DATE_FORMAT));
        LocalTime startTime = LocalTime.parse(sessionDto.getStartTime(), TIME_FORMATTER);
        sessionEntity.setStartTime(startTime);
        sessionEntity.setEndTime(startTime.plusMinutes(movieEntity.getDuration() + 15));
        sessionEntity.setVirtualActive(sessionDto.getVirtualActive());
        sessionEntity.setActive(sessionDto.getActive());
        //generateTicketsForSession(sessionEntity);
        createSession(sessionEntity);


    }

    public void generateSessionsForOneFilmForOneHallEnd(Long id) {
        boolean act;
        SessionEntity sessionEntity = sessionRepository.getOne(id);

        for (LocalTime i = sessionEntity.getEndTime(); i.isBefore(LocalTime.of(23, 0, 0)); ) {
            SessionEntity sessionEntity1 = new SessionEntity();
            MovieEntity movieEntity = sessionEntity.getMovie();
            sessionEntity1.setMovie(movieEntity);
            HallEntity hallEntity = sessionEntity.getHall();
            sessionEntity1.setHall(hallEntity);
            sessionEntity1.setDate(sessionEntity.getDate());
            LocalTime startTime = i;
            sessionEntity1.setStartTime(startTime);
            LocalTime endTime = (startTime.plusMinutes(movieEntity.getDuration() + 15));
            sessionEntity1.setEndTime(endTime);
            sessionEntity1.setVirtualActive(sessionEntity.getVirtualActive());
            sessionEntity1.setActive(sessionEntity.getActive());
            if (isOpen(LocalTime.of(6, 0, 0), LocalTime.of(23, 0, 0), startTime)) {
                createSession(sessionEntity1);
            } else
                break;
            i = endTime;


        }


    }

    public void copySessionsForOneWeek(String localDate) {
        List<SessionEntity> sessionEntities = sessionRepository.findAllByDate(LocalDate.parse(localDate));

        for (int i = 1; i <= 7; i++) {
            for (SessionEntity sessionEntity : sessionEntities
                    ) {
                SessionEntity sessionEntity1 = new SessionEntity();
                sessionEntity1.setMovie(sessionEntity.getMovie());
                sessionEntity1.setHall(sessionEntity.getHall());
                sessionEntity1.setStartTime(sessionEntity.getStartTime());
                sessionEntity1.setEndTime(sessionEntity.getEndTime());
                sessionEntity1.setVirtualActive(sessionEntity.getVirtualActive());
                sessionEntity1.setActive(sessionEntity.getActive());
                LocalDate localDate1 = sessionEntity.getDate();
                sessionEntity1.setDate(localDate1.plusDays(i));
                sessionRepository.save(sessionEntity1);


            }
        }

    }

    @Override
    public BigDecimal getBasePrice(SessionEntity sessionEntity) {
        MovieEntity movieEntity = sessionEntity.getMovie();
        LocalDate sessionDay = sessionEntity.getDate();
        List<BigDecimal> coefs = new ArrayList<>();

        coefs.add(getPremierCoef(movieEntity, sessionDay));
        coefs.add(getEndRentCoef(movieEntity, sessionDay));
        coefs.add(CoefficientType.BASE.getValue());

        BigDecimal total = movieEntity.getPrice();
        for (BigDecimal coef : coefs) {
            total = total.multiply(coef, mc);
        }
        return roundPrice(total);
    }

    @Override
    public BigDecimal getVipPrice(SessionEntity sessionEntity) {
        BigDecimal price =  getBasePrice(sessionEntity).multiply(CoefficientType.VIP.getValue(), mc);
        return roundPrice(price);
    }

    @Override
    public BigDecimal getVirtualPrice(SessionEntity sessionEntity) {
        BigDecimal price = getBasePrice(sessionEntity).multiply(CoefficientType.VIRTUAL.getValue(), mc);
        return roundPrice(price);
    }

    @Override
    public List<BigDecimal> getCoefs(MovieEntity movieEntity, LocalDate sessionDay, SeatEntity seatEntity) {
        List<BigDecimal> coefs = new ArrayList<>();
        coefs.add(getPremierCoef(movieEntity, sessionDay));
        coefs.add(getEndRentCoef(movieEntity, sessionDay));
        coefs.add(getBaseSeatCoef(seatEntity));
        coefs.add(getVipSeatCoef(seatEntity));
        coefs.add(getVirtualSeatCoef(seatEntity));
        return coefs;
    }

    public boolean isBetweenTwoDates(LocalDate start, LocalDate end, LocalDate sessionDay) {

        boolean result = false;

        if ((sessionDay.isAfter(start)) && (sessionDay.isBefore(end))) {
            result = true;
        }
        return result;
    }

    public BigDecimal getPremierCoef(MovieEntity movieEntity, LocalDate sessionDay) {

        BigDecimal coef = CoefficientType.DEF.getValue();
        LocalDate startPremierTime = movieEntity.getStartRent();
        LocalDate endPremierTime = startPremierTime.plusWeeks(1);

        if (isBetweenTwoDates(startPremierTime, endPremierTime, sessionDay)) {
            coef = CoefficientType.PREMIER.getValue();
        }
        return coef;
    }

    public BigDecimal getEndRentCoef(MovieEntity movieEntity, LocalDate sessionDay) {

        BigDecimal coef = CoefficientType.DEF.getValue();
        LocalDate endRent = movieEntity.getEndRent();
        LocalDate fiveDays = endRent.minusDays(5);

        if (isBetweenTwoDates(fiveDays, endRent, sessionDay)) {
            coef = CoefficientType.END.getValue();
        }
        return coef;
    }

    public BigDecimal getVipSeatCoef(SeatEntity seatEntity) {

        BigDecimal coef = CoefficientType.DEF.getValue();
        if (seatEntity.getType().equalsIgnoreCase(SeatType.VIP.getType())) {
            coef = CoefficientType.VIP.getValue();
        }
        return coef;
    }

    public BigDecimal getBaseSeatCoef(SeatEntity seatEntity) {

        BigDecimal coef = CoefficientType.DEF.getValue();
        if (seatEntity.getType().equalsIgnoreCase(SeatType.BASE.getType())) {
            coef = CoefficientType.BASE.getValue();
        }
        return coef;
    }

    public BigDecimal getVirtualSeatCoef(SeatEntity seatEntity) {

        BigDecimal coef = CoefficientType.DEF.getValue();
        if (seatEntity.getType().equalsIgnoreCase(SeatType.VIRTUAL.getType())) {
            coef = CoefficientType.VIRTUAL.getValue();
        }
        return coef;
    }

    @Override
    public boolean isOpen(LocalTime start, LocalTime end, LocalTime time) {
        boolean result = false;

        if ((time.isAfter(start)) && (time.isBefore(end))) {
            result = true;
        }
        return result;
    }

    @Override
    public List<Schedule> schedule(){
        List<Schedule> schedules = new LinkedList<>();
        List<MovieEntity> disMovieList = sessionRepository.getDisMovies();
        for(int i = 0; i < disMovieList.size(); i++){
           Long id = disMovieList.get(i).getId();
            for(int q = 0 ; q < sessionRepository.getDisDates(id).size(); q++){
                Schedule schedule = new Schedule();
                schedule.setLocalDate(sessionRepository.getDisDates(id).get(q));
                schedule.setSessionEntityList(scheduleConverter.convertToDto(sessionRepository.getDisStatrtTimes(id,schedule.getLocalDate())));
                schedule.setTitle(sessionRepository.findById(schedule.getSessionEntityList().get(0).getId()).orElse(null).getMovie().getTitle());
                schedule.setMovieId(sessionRepository.findById(schedule.getSessionEntityList().get(0).getId()).orElse(null).getMovie().getId());
                schedules.add(schedule);
            }
        }
        return schedules;
    }

    @Override
    public List<SessionEntity> getAllByActive(boolean b) {
      return sessionRepository.findAllByActive(true);
    }

    private BigDecimal roundPrice(BigDecimal price){
        return price.setScale(priceScale, RoundingMode.UP);
    }

}

