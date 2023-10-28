package raf.sk_schedule;


import raf.sk_schedule.api.ScheduleManager;
import raf.sk_schedule.exception.ScheduleException;
import raf.sk_schedule.filter.SearchCriteria;
import raf.sk_schedule.model.RoomProperties;
import raf.sk_schedule.model.ScheduleSlot;
import raf.sk_schedule.util.RoomPropertiesImporterCSV;
import raf.sk_schedule.util.ScheduleSlotImporterCSV;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class ScheduleSlotsManager implements ScheduleManager {
    private Date startingDate;
    private Date endingDate;
    private List<ScheduleSlot> mySchedule;
    private Map<String, RoomProperties> rooms;



    public ScheduleSlotsManager() {
        mySchedule = new ArrayList<>();
        rooms = new HashMap<>();
    }


    @Override
    public void initialize(Date startingDate, Date endDate) {
        this.startingDate = startingDate;
        this.endingDate = endDate;
    }

    public int loadRoomsSCV(String csvPath) throws IOException {
        rooms = RoomPropertiesImporterCSV.importRoomsCSV(csvPath);
        return rooms.size();
    }

    public int loadScheduleSCV(String csvPath) throws IOException {
        if(rooms.isEmpty())
            throw new ScheduleException("Your room properties are currently empty. You need to import them first in order to bind the scheduled slots with their location.");

        try {
            mySchedule = ScheduleSlotImporterCSV.importRoomsCSV(csvPath, rooms);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return mySchedule.size();
    }

    @Override
    public List<RoomProperties> getAllRooms() {
        return new ArrayList<RoomProperties>(rooms.values());
    }

    public void addRoom(RoomProperties roomProperties) {

        if (rooms.containsKey(roomProperties.getName()))

            throw new ScheduleException(
                    "Room with selected name already exists, if u want to change existing room properties please use updateRoom method from the schedule api.");

        rooms.put(roomProperties.getName(), roomProperties);
    }

    public boolean hasRoom(String roomName) {
              return rooms.containsKey(roomName);
    };

    public void updateRoom(String name, RoomProperties newProp) {


        if (!rooms.containsKey(name))
            throw new ScheduleException("Room with a name: " + name + "does not exist in schedule.");


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
                        "The room: "
                                + curr.getLocation().getName()
                                + " is already scheduled between "
                                + curr.getStartAsString().split(" +")[1] +
                                " and " + curr.getEndAsString().split("\\s")[1] +
                                " on: " + curr.getStartAsString().split(" +")[0]);
        }

        return mySchedule.add(timeSlot);
    }


    @Override
    public boolean deleteTimeSlot(ScheduleSlot timeSlot) {


        for (ScheduleSlot curr : mySchedule) {
            if (curr.getStart().getTime() == timeSlot.getEnd().getTime() &&
                    curr.getDuration() == timeSlot.getDuration())
                return mySchedule.remove(curr);
        }

        return false;
    }

    @Override
    public void moveTimeSlot(ScheduleSlot oldTimeSlot, ScheduleSlot newTimeSlot) {
        if (isTimeSlotAvailable(newTimeSlot)) {
            for (ScheduleSlot curr : mySchedule) {

            }
        }
    }

    @Override
    public boolean isTimeSlotAvailable(ScheduleSlot timeSlot) {
        for (ScheduleSlot curr : mySchedule) {
            try {
                if (curr.isCollidingWith(timeSlot))
                    return false;
            } catch (ParseException e) {
                throw new ScheduleException(e);
            }
        }
        return true;
    }

    @Override
    public List<ScheduleSlot> getFreeTimeSlots(Date startDate, Date endDate) {
        // Implement logic to find free time slots within the specified date range
        List<ScheduleSlot> freeTimeSlots = new ArrayList<>();
        // Add your logic here
        return freeTimeSlots;
    }

    @Override
    public List<ScheduleSlot> searchTimeSlots(SearchCriteria criteria) {
        // Implement logic to search for time slots based on criteria
        List<ScheduleSlot> matchingTimeSlots = new ArrayList<>();
        // Add your logic here
        return matchingTimeSlots;
    }

    @Override
    public void loadScheduleFromFile(String filePath) {
        // Implement logic to load the schedule from a file
    }

    @Override
    public void saveScheduleToFile(String filePath) {
        // Implement logic to save the schedule to a file
    }

    @Override
    public void exportScheduleCSV(Date date, Date date1) {

    }

    @Override
    public void exportScheduleJSON(Date date, Date date1) {

    }

    @Override
    public void exportFilteredScheduleCSV(SearchCriteria searchCriteria, Date date, Date date1) {

    }

    @Override
    public void exportFilteredScheduleJSON(SearchCriteria searchCriteria, Date date, Date date1) {

    }


    @Override
    public List<ScheduleSlot> getSchedule(Date date, Date date1) {
        return null;
    }

    @Override
    public List<ScheduleSlot> getWholeSchedule() {
        return mySchedule;
    }
}
