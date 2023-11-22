package raf.sk_schedule;

import raf.sk_schedule.api.Constants.WeekDay;
import raf.sk_schedule.api.ScheduleManagerAdapter;
import raf.sk_schedule.exception.ScheduleException;
import raf.sk_schedule.model.location_node.RoomProperties;
import raf.sk_schedule.model.schedule_mapper.RepetitiveScheduleMapper;
import raf.sk_schedule.model.schedule_node.FreeScheduleSlot;
import raf.sk_schedule.model.schedule_node.ScheduleSlot;
import raf.sk_schedule.util.exporter.ScheduleExporterCSV;
import raf.sk_schedule.util.exporter.ScheduleExporterJSON;
import raf.sk_schedule.util.filter.CriteriaFilter;
import raf.sk_schedule.util.filter.SearchCriteria;
import raf.sk_schedule.util.importer.ScheduleImporter;
import raf.sk_schedule.util.sort.ScheduleSlotSorter;

import java.io.File;
import java.util.*;

import static raf.sk_schedule.util.date_formater.DateTimeFormatter.formatDate;
import static raf.sk_schedule.util.date_formater.DateTimeFormatter.parseDate;
import static raf.sk_schedule.util.persistence.ScheduleFileOperationUnit.initializeFile;
import static raf.sk_schedule.util.persistence.ScheduleFileOperationUnit.writeStringToFile;
import static raf.sk_schedule.util.sort.ScheduleSlotComparator.ASCENDING_ORDER;

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

    // TODO: done
    @Override
    public int loadRoomsSCV(String csvPath) {
        rooms = ScheduleImporter.importRoomsCSV(csvPath);
        return rooms.size();
    }

    // TODO: done
    @Override
    public int loadScheduleSCV(String csvPath) {
        if (rooms.isEmpty())
            throw new ScheduleException("Your room properties are currently empty. You need to import them first in order to bind the scheduled slots with their location.");

        mySchedule = ScheduleImporter.importScheduleCSV(csvPath, rooms);
        return mySchedule.size();
    }

    // TODO: done
    @Override
    public List<RoomProperties> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    // TODO: done
    public void addRoom(RoomProperties roomProperties) {

        if (rooms.containsKey(roomProperties.getName()))

            throw new ScheduleException(
                    "Room with selected name already exists, if you want to change existing room properties please use updateRoom method from the schedule api.");

        rooms.put(roomProperties.getName(), roomProperties);
    }

    // TODO: done
    @Override
    public boolean hasRoom(String roomName) {
        return rooms.containsKey(roomName);
    }

    // TODO: done
    @Override
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


    // TODO: done
    @Override
    public boolean deleteRoom(String roomName) {

        if (!rooms.containsKey(roomName))
            throw new ScheduleException("There is no room with that name in schedule.");

        // remove all the slots that are booked in that room
        mySchedule.removeIf(slot -> slot.getLocation().equals(rooms.get(roomName)));

        // remove the room itself
        rooms.remove(roomName);

        return true;
    }

    // TODO: done
    @Override
    public RoomProperties getRoomByName(String name) {
        return rooms.get(name);
    }

    // TODO: done
    public List<RoomProperties> roomLookUp(String name, int capacity, int hasComputers, Boolean hasProjector, Map<String, String> attributes) {

        List<RoomProperties> lookUpResult = new ArrayList<>();
        boolean include;

        // iterate through rooms
        for (RoomProperties room : rooms.values()) {

            // perform attributes lookup first if attributes are queried
            if (attributes != null) {
                include = true;
                // iterate through queried attributes
                for (String attribute : attributes.keySet()) {

                    // check if the queried attributes exists inside currently iterated room and does the queried attributes values match
                    if (room.hasAttribute(attribute) && !room.getAttribute(attribute).equals(attributes.get(attribute))) {
                        include = false;
                        break;
                    }
                }

                // based on attributes names and values lookup we determine weather the room should be included in lookUp result, if not  jump to next room
                if (!include)
                    continue;

            }

            if ((name != null && !room.getName().equals(name))
                    ||
                    (capacity >= 0 && room.getCapacity() != capacity)
                    ||
                    (hasComputers >= 0 && room.hasComputers() != hasComputers)
                    ||
                    (hasProjector != null && room.hasProjector() != hasProjector)
            )

                continue;

            // since we touched down this line of code the room passed the lookup query requirements and is included in lookUpResult
            lookUpResult.add(room);

        }
        return lookUpResult;
    }

    // TODO: done
    @Override
    public boolean bookScheduleSlot(ScheduleSlot timeSlot) throws ScheduleException {
        //check if there is collision with any of the existing slots

        for (ScheduleSlot curr : mySchedule) {
            if (curr.isCollidingWith(timeSlot))
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
    public List<ScheduleSlot> bookRepetitiveScheduleSlot(String startTime, int duration, String endTime, WeekDay weekDay, int recurrencePeriod, String schedulingIntervalStart, String schedulingIntervalEnd) {
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

        List<ScheduleSlot> mappedSlots = mapperBuilder.build().mapSchedule();

        // nullptr safety handle return empty list
        if (mappedSlots == null)
            return new ArrayList<>();

        //check for collisions before booking
        for (ScheduleSlot bookedSlot : mySchedule) {
            for (ScheduleSlot toBeBookedSlot : mappedSlots) {
                if (bookedSlot.isCollidingWith(toBeBookedSlot)) {
                    throw new ScheduleException("The action: scheduleRepetitiveTimeSlot could not be executed because "
                            + "the collision with an existing slot that occupies the time window on date:" + formatDate(bookedSlot.getDate())
                            + " between " + bookedSlot.getStartTime() + " and " + bookedSlot.getEndTime() + ".");
                }
            }
        }
        // booking mapped slots
        mySchedule.addAll(mappedSlots);
        // return the slots that have been booked
        return mappedSlots;
    }

    // TODO: done
    @Override
    public List<ScheduleSlot> bookRepetitiveScheduleSlot(RepetitiveScheduleMapper repetitiveScheduleMapper) {
        List<ScheduleSlot> mappedSlots = repetitiveScheduleMapper.mapSchedule();

        for (ScheduleSlot bookedSlot : mySchedule) {
            for (ScheduleSlot toBeBookedSlot : mappedSlots) {
                if (bookedSlot.isCollidingWith(toBeBookedSlot)) {
                    throw new ScheduleException("The action: scheduleRepetitiveTimeSlot could not be executed because "
                            + "the collision with an existing slot that occupies the time window on date:" + formatDate(bookedSlot.getDate())
                            + " between " + bookedSlot.getStartTime() + " and " + bookedSlot.getEndTime() + " . ");
                }
            }
        }

        mySchedule.addAll(mappedSlots);
        return mappedSlots;
    }


    // TODO: done
    @Override
    public List<ScheduleSlot> deleteScheduleSlot(ScheduleSlot timeSlot) throws ScheduleException {
        List<ScheduleSlot> removedSlots = new ArrayList<>();
        for (ScheduleSlot slot : mySchedule) {
            if (slot.equals(timeSlot)) {
                mySchedule.remove(slot);
                if (slot.getSharedState() != null)
                    slot.getSharedState().removeLinkedSlot(slot);
                removedSlots.add(slot);
                return removedSlots;
            }
        }

        throw new ScheduleException("The slot with the specified time/location properties was not found in schedule.");
    }

    public void moveScheduleSlot(ScheduleSlot scheduleSlot, Object newDate, String newStartTime, int newDuration, String newEndTime) {

        for (ScheduleSlot curr : mySchedule) {
            if (curr.equals(scheduleSlot)) {

            }
        }

    }

    // TODO: done
    @Override
    public List<ScheduleSlot> isScheduleSlotAvailable(ScheduleSlot timeSlot) {

        List<ScheduleSlot> collisions = new ArrayList<>();
        for (ScheduleSlot curr : mySchedule) {
            if (curr.isCollidingWith(timeSlot))
                collisions.add(curr);
        }
        return collisions;

    }


    // TODO: done
    public List<ScheduleSlot> isScheduleSlotAvailable(Object date, String startTime, String endTime, String location) {

        /* slots are bound by location dimension */
        if (!rooms.containsKey(location))
            throw new ScheduleException("Schedule model does not contain the room with the name: " + location + ".");

        /*
         Build out the slot, so we can easily check if there is collision,
         and if not that means that the slot is available for scheduling
         */
        ScheduleSlot slot = new ScheduleSlot.Builder()
                .setDate(date instanceof Date ? (Date) date : parseDate(date.toString()))
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLocation(rooms.get(location))

                .build();
        //result list
        List<ScheduleSlot> collisions = new ArrayList<>();

        // detect all the collisions
        for (ScheduleSlot curr : mySchedule) {
            if (curr.isCollidingWith(slot))
                collisions.add(curr);
        }

        // if there are no collisions the list will be empty
        return collisions;
    }

    // TODO: LAST
    @Override
    public List<FreeScheduleSlot> getFreeScheduleSlots(Object lowerDateBound, Object upperDateBound) {

        throw new RuntimeException("NIJE GOTOVOOOOOOOOOOOOOOOOOO x,X' !");
    }


    // TODO: done
    @Override
    public List<ScheduleSlot> searchScheduleSlots(SearchCriteria criteria) {

        /*
        I can ignore the return value of filter() method call because the passed modelState list
        is automatically changed  via reference that is sent as an argument
        */
        return criteria.filter(new ArrayList<>(mySchedule));
    }


    // TODO: done
    @Override
    public int exportScheduleCSV(String filePath, Object lowerDateBound, Object upperDateBound, String... includedAttributes) {

        // configure file
        File file = initializeFile(filePath);

        // extract the data
        List<ScheduleSlot> schedule = getSchedule(lowerDateBound, upperDateBound);

        // sort data (default chronological order)
        schedule = ScheduleSlotSorter.sortByAbsoluteStartTime(schedule, ASCENDING_ORDER);

        /* ScheduleComponentAPI Util default CSV serialization */
        String serializedList = ScheduleExporterCSV.listToCSV(schedule, includedAttributes);

        // append argument is false meaning that we want to delete previous file contents if there was any and then write our string
        writeStringToFile(file, serializedList, false);

        /* return number of exported rows */
        return schedule.size();
    }


    // TODO: done
    @Override
    public int exportFilteredScheduleCSV(String filePath, SearchCriteria searchCriteria, String... includedAttributes) {

        // configure file
        File file = initializeFile(filePath);

        // extract the data
        List<ScheduleSlot> searchResult = searchCriteria.filter(new ArrayList<>(mySchedule));

        // sort data (default chronological order)
        searchResult = ScheduleSlotSorter.sortByAbsoluteStartTime(searchResult, ASCENDING_ORDER);

        // serialize data
        String csv = ScheduleExporterCSV.listToCSV(searchResult, includedAttributes);

        // persisting the result (writing to file)
        writeStringToFile(file, csv, true);

        return searchResult.size();
    }


    // TODO: done
    @Override
    public int exportScheduleJSON(String filePath, Object lowerDateBound, Object upperDateBound) {

        // configure file
        File file = initializeFile(filePath);

        // extract the data
        List<ScheduleSlot> schedule = getSchedule(lowerDateBound, upperDateBound);

        //serialize data
        String serializedList = ScheduleExporterJSON.serializeObject(schedule);

        //persist serialize data inside the file
        writeStringToFile(file, serializedList, true);

        //return serialized objects count
        return schedule.size();

    }


    // TODO: done
    @Override
    public int exportFilteredScheduleJSON(String filePath, SearchCriteria searchCriteria) {

        // configure file
        File file = initializeFile(filePath);

        // filter data
        List<ScheduleSlot> searchResult = searchCriteria.filter(new ArrayList<>(mySchedule));

        // sort data (default chronological order)
        searchResult = ScheduleSlotSorter.sortByAbsoluteStartTime(searchResult, ASCENDING_ORDER);

        //serialize data
        String serializedList = ScheduleExporterJSON.serializeObject(searchResult);

        //persist serialize data inside the file
        writeStringToFile(file, serializedList, false);

        //return serialized objects count with null ptr exception safety check
        return searchResult != null ? searchResult.size() : 0;
    }


    // TODO: done
    @Override
    public List<ScheduleSlot> getSchedule(String lowerBoundDate, String upperBoundDate) {
        // Using search criteria utility of SchedulingComponentAPI and setting lower and upper date bounds.
        return new SearchCriteria.Builder()
                .setCriteria(CriteriaFilter.LOWER_BOUND_DATE_KEY, lowerBoundDate)
                .setCriteria(CriteriaFilter.UPPER_BOUND_DATE_KEY, upperBoundDate)
                .build()
                .filter(new ArrayList<>(mySchedule));
    }


    // TODO: done

    // TODO: done
    @Override
    public List<ScheduleSlot> getSchedule(Object lowerBoundDate, Object upperBoundDate) {

        return ScheduleSlotSorter.sortByAbsoluteStartTime(
                // perform upper/lower date bound schedule filtering
                new SearchCriteria.Builder()
                        // if lowerBoundDate is null we go from start
                        .setCriteria(CriteriaFilter.LOWER_BOUND_DATE_KEY, lowerBoundDate == null ? super.startingDate : lowerBoundDate)
                        // if upperBoundDate is null we go till the end
                        .setCriteria(CriteriaFilter.UPPER_BOUND_DATE_KEY, upperBoundDate == null ? super.endingDate : upperBoundDate)
                        .build()
                        .filter(new ArrayList<>(mySchedule))
                // perform sorting by absolute start time (default chronological order)
                , ASCENDING_ORDER
        );
    }

    @Override
    public List<ScheduleSlot> getWholeSchedule() {

        // perform sorting by absolute start time (default chronological order)
        return ScheduleSlotSorter.sortByAbsoluteStartTime(new ArrayList<>(mySchedule), ASCENDING_ORDER);
    }


}
