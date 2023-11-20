package raf.sk_schedule;

import raf.sk_schedule.api.Constants.WeekDay;
import raf.sk_schedule.api.ScheduleManagerAdapter;
import raf.sk_schedule.exception.ScheduleException;

import raf.sk_schedule.model.location_node.RoomProperties;
import raf.sk_schedule.model.schedule_mapper.RepetitiveScheduleMapper;
import raf.sk_schedule.model.schedule_node.ScheduleSlot;
import raf.sk_schedule.util.exporter.ScheduleExporterCSV;
import raf.sk_schedule.util.exporter.ScheduleExporterJSON;
import raf.sk_schedule.util.filter.CriteriaFilter;
import raf.sk_schedule.util.filter.SearchCriteria;
import raf.sk_schedule.util.importer.ScheduleImporter;

import java.io.File;
import java.text.ParseException;
import java.util.*;

import static raf.sk_schedule.util.date_formater.DateTimeFormatter.parseDate;
import static raf.sk_schedule.util.persistence.ScheduleFileOperationUnit.initializeFile;
import static raf.sk_schedule.util.persistence.ScheduleFileOperationUnit.writeStringToFile;

public class ScheduleSlotsManager extends ScheduleManagerAdapter {

    private Date startingDate;
    private Date endingDate;
    private List<ScheduleSlot> mySchedule;

    private List<RepetitiveScheduleMapper> repetitiveSchedule;
    private Map<String, RoomProperties> rooms;


    public ScheduleSlotsManager() {
        mySchedule = new ArrayList<>();
        rooms = new HashMap<>();
    }


    public int loadRoomsSCV(String csvPath) {
        rooms = ScheduleImporter.importRoomsCSV(csvPath);
        return rooms.size();
    }

    public int loadScheduleSCV(String csvPath) {
        if (rooms.isEmpty())
            throw new ScheduleException("Your room properties are currently empty. You need to import them first in order to bind the scheduled slots with their location.");

        mySchedule = ScheduleImporter.importScheduleCSV(csvPath, rooms);
        return mySchedule.size();
    }

    @Override
    public List<RoomProperties> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }


    public void addRoom(RoomProperties roomProperties) {

        if (rooms.containsKey(roomProperties.getName()))

            throw new ScheduleException(
                    "Room with selected name already exists, if you want to change existing room properties please use updateRoom method from the schedule api.");

        rooms.put(roomProperties.getName(), roomProperties);
    }

    public boolean hasRoom(String roomName) {
        return rooms.containsKey(roomName);
    }

    public void updateRoom(String name, RoomProperties newProp) {

        if (!rooms.containsKey(name))
            throw new ScheduleException("Room with a name: " + name + " does not exist in schedule.");

        if (!name.equals(newProp.getName())) {

            if (rooms.containsKey(newProp.getName()))
                throw new ScheduleException("You can not change room: " + name + " to " + newProp + " because room with that name already exists.\n" +
                        "If you really want to make this change you can change the room: " + newProp.getName() + " name to something else, than set room:. " + name + " to " + newProp.getName());
        }
        rooms.put(name, newProp);
    }

    @Override
    public void deleteRoom(String s) {

        if (!rooms.containsKey(s))
            throw new ScheduleException("There is no room with that name in schedule.");
        rooms.remove(s);
    }

    @Override
    public RoomProperties getRoom(String name) {
        return rooms.get(name);
    }


    @Override
    public boolean scheduleTimeSlot(ScheduleSlot timeSlot) throws ParseException, ScheduleException {
        //check if there is collision with any of the existing slots

        for (ScheduleSlot curr : mySchedule) {
            if (curr.isCollidingWith(timeSlot) && curr.getLocation().getName().equals(timeSlot.getLocation().getName()))
                throw new ScheduleException(
                        "The room: " + curr.getLocation().getName()
                                + " is already scheduled between " + curr.getStartTime()
                                + " and " + curr.getEndTime()
                                + " on date: " + curr.getDate()
                );
        }
        return mySchedule.add(timeSlot);
    }

    @Override
    public boolean scheduleRepetitiveTimeSlot(String startTime, int duration, String endTime, WeekDay weekDay, int recurrencePeriod, String schedulingIntervalStart, String schedulingIntervalEnd) {
        RepetitiveScheduleMapper.Builder mapperBuilder = new RepetitiveScheduleMapper.Builder()
                .setStartTime(startTime)
                .setRecurrenceIntervalStart(parseDate(schedulingIntervalStart))
                .setRecurrenceIntervalEnd(parseDate(schedulingIntervalEnd));

        if (endTime != null)
            mapperBuilder.setEndTime(endTime);

        else if (duration > 0)
            mapperBuilder.setDuration(duration);

        if (weekDay != null)
            mapperBuilder.setWeekDay(weekDay);

        if (recurrencePeriod > 0)
            mapperBuilder.setRecurrencePeriod(recurrencePeriod);

        return false;
    }

    @Override
    public boolean scheduleRepetitiveTimeSlot(RepetitiveScheduleMapper repetitiveScheduleMapper) {

        repetitiveScheduleMapper.mapSchedule();
        return true;
    }


    @Override
    public boolean deleteTimeSlot(ScheduleSlot timeSlot) {
        return false;
    }

    public void moveTimeSlot(ScheduleSlot oldTimeSlot, ScheduleSlot newTimeSlot) {

    }

    @Override
    public boolean isTimeSlotAvailable(ScheduleSlot timeSlot) {

        for (ScheduleSlot slot : mySchedule) {
            if (slot.isCollidingWith(timeSlot))
                return false;
        }
        return true;

    }

    public boolean isTimeSlotAvailable(String date, String startTime, String endTime, String location) {
        /* slots are bound by location dimension */
        if (!rooms.containsKey(location))
            throw new ScheduleException("Schedule model does not contain the room with the name: " + location + ".");

        /*
         Build out the slot, so we can easily check if there is collision,
         and if not that means that the slot is available for scheduling
         */
        ScheduleSlot slot = new ScheduleSlot.Builder()
                .setDate(parseDate(date))
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLocation(rooms.get(location))
                .build();

        for (ScheduleSlot curr : mySchedule) {
            if (slot.isCollidingWith(slot))
                return false;
        }
        return true;

    }


    @Override
    public List<ScheduleSlot> getFreeTimeSlots(String startDate, String endDate) {

        throw new RuntimeException("NIJE GOTOVOOOOOOOOOOOOOOOOOO x,X' !");
    }


    @Override
    public List<ScheduleSlot> searchTimeSlots(SearchCriteria criteria) {
        List<ScheduleSlot> modelState = new ArrayList<>(mySchedule);
        /*
        I can ignore the return value of filter() method call because the passed modelState list
        is automatically changed  via reference that is sent as an argument
        */
        criteria.filter(modelState);

        /* return computed list */
        return modelState;
    }


    @Override
    public int exportScheduleCSV(String filePath, String firstDate, String lastDate) {

        /* using my own getSchedule() to bound exporting schedule, so I don't have to write same thing twice :v) */
        List<ScheduleSlot> schedule = getSchedule(firstDate, lastDate);

        /* Export to file... */
        File file = initializeFile(filePath);

        /* ScheduleComponentAPI Util default csv serialization */
        String serializedList = ScheduleExporterCSV.listToCSV(schedule);

        /* append param is false since */
        writeStringToFile(file, serializedList, false);

        /* return number of exported rows */
        return schedule.size();
    }


    @Override
    public int exportFilteredScheduleCSV(String filePath, SearchCriteria searchCriteria) {
        return 1;
    }


    @Override
    public int exportScheduleJSON(String filePath, String lowerDateBound, String upperDateBound) {

        // configure file
        File file = initializeFile(filePath);

        // extract the data
        List<ScheduleSlot> schedule = getSchedule(
                lowerDateBound == null ? super.startingDate : parseDate(lowerDateBound),
                upperDateBound == null ? super.endingDate : parseDate(upperDateBound)
        );

        //serialize data
        String serializedList = ScheduleExporterJSON.serializeObject(schedule);

        //persist serialize data inside the file
        writeStringToFile(file, serializedList, true);

        //return serialized objects count
        return schedule.size();

    }

    @Override
    public int exportFilteredScheduleJSON(String filePath, SearchCriteria searchCriteria) {

        // configure file
        File file = initializeFile(filePath);

        // extract the data
        List<ScheduleSlot> searchResult = searchTimeSlots(searchCriteria);

        //serialize data
        String serializedList = ScheduleExporterJSON.serializeObject(searchResult);

        //persist serialize data inside the file
        writeStringToFile(file, serializedList, false);

        //return serialized objects count with null ptr exception safety
        return searchResult != null ? searchResult.size() : 0;
    }

    @Override
    public List<ScheduleSlot> getSchedule(String lowerBoundDate, String upperBoundDate) {
        return new SearchCriteria.Builder()
                .setCriteria(CriteriaFilter.LOWER_BOUND_DATE_KEY, lowerBoundDate)
                .setCriteria(CriteriaFilter.UPPER_BOUND_DATE_KEY, upperBoundDate)
                .build()
                .filter(new ArrayList<>(mySchedule));
    }

    @Override
    public List<ScheduleSlot> getSchedule(Date lowerBoundDate, Date upperBoundDate) {
        return new SearchCriteria.Builder()
                .setCriteria(CriteriaFilter.LOWER_BOUND_DATE_KEY, lowerBoundDate)
                .setCriteria(CriteriaFilter.UPPER_BOUND_DATE_KEY, upperBoundDate)
                .build()
                .filter(new ArrayList<>(mySchedule));
    }

    @Override
    public List<ScheduleSlot> getWholeSchedule() {
        return mySchedule;
    }


}
