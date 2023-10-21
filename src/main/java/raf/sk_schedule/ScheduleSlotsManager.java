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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScheduleSlotsManager implements ScheduleManager {
    private List<ScheduleSlot> timeSlots;
    private List<RoomProperties> rooms;


    public ScheduleSlotsManager() {
        timeSlots = new ArrayList<>();
        rooms = new ArrayList<>();
    }

    @Override
    public void initialize() {
        timeSlots = new ArrayList<>();
        rooms = new ArrayList<>();
    }

    @Override
    public void initialize(Date startingDate, Date endDate) {

    }

    public void loadRoomsSCV(String csvPath) throws IOException {

        rooms = RoomPropertiesImporterCSV.importRoomsCSV(csvPath);

    }

    public void loadScheduleSCV(String csvPath) throws IOException {
        List<ScheduleSlot> slots = ScheduleSlotImporterCSV.importRoomsCSV(csvPath);
    }


    public boolean addRoom(RoomProperties roomProperties) {
        for (RoomProperties room : rooms) {
            if (room.getName().equals(roomProperties.getName()))
                throw new ScheduleException(
                        "Room with selected name already exists, if u want to change existing room properties please use updateRoom method from the schedule api.");
        }
        return rooms.add(roomProperties);
    }

    public boolean updateRoom(String name, RoomProperties newProp) {

        RoomProperties updatedRoom = getRoomProperties(name);

        if (updatedRoom == null)
            throw new ScheduleException("Room with a name: " + name + "does not exist in schedule.");


        if (!name.equals(newProp.getName())) {
            RoomProperties isOccupied = getRoomProperties(newProp.getName());

            if (isOccupied != null)
                throw new ScheduleException("You can not change room: " + name + " to " + newProp + " because room with that name already exists.\n" +
                        "If you really want to make this change you can change the room: " + isOccupied.getName() + " name to something else, than set room:. " + name + " to " + newProp.getName());

            updatedRoom.setName(newProp.getName());
        }
        if (newProp.getCapacity() > -1)
            updatedRoom.setCapacity(newProp.getCapacity());

        updatedRoom.setHasComputers(newProp.hasComputers());
        updatedRoom.setHasProjector(newProp.hasProjector());
        updatedRoom.setExtra(newProp.getExtra());
        return true;

    }

    @Override
    public boolean deleteRoom(String s) {
        for (RoomProperties curr : rooms) {
            if (curr.getName().equals(s))
                return rooms.remove(curr);
        }
        throw new ScheduleException("There is no room with that name in schedule.");
    }

    public RoomProperties getRoomProperties(String name) {
        for (RoomProperties room : rooms) {
            if (room.getName().equals(name))
                return room;
        }
        return null;
    }

    @Override
    public boolean addTimeSlot(ScheduleSlot timeSlot) throws ParseException, ScheduleException {
        //check if there is collision with any of the existing slots
        for (ScheduleSlot curr : timeSlots) {
            if (curr.isCollidingWith(timeSlot) && curr.getLocation().getName().equals(timeSlot.getLocation().getName()))
                throw new ScheduleException("The room: " + curr.getLocation().getName() + " is already scheduled between "
                        + curr.getStartAsString().split(" +")[1] + " and " + curr.getEndAsString().split("\\s")[1] +
                        " on: " + curr.getStartAsString().split(" +")[0]);
        }

        return timeSlots.add(timeSlot);
    }


    @Override
    public boolean deleteTimeSlot(ScheduleSlot timeSlot) {

        try {
            for (ScheduleSlot curr : timeSlots) {
                if (curr.startTime() == timeSlot.startTime() && curr.endTime() == timeSlot.endTime())
                    return timeSlots.remove(curr);

            }
        } catch (ParseException e) {
            throw new ScheduleException("Date parsing failed, check date and time format.");
        }

        return false;
    }

    @Override
    public void moveTimeSlot(ScheduleSlot oldTimeSlot, ScheduleSlot newTimeSlot) {
        // Move a time slot
        if (timeSlots.contains(oldTimeSlot)) {
            timeSlots.remove(oldTimeSlot);
            timeSlots.add(newTimeSlot);
        }
    }

    @Override
    public boolean isTimeSlotAvailable(ScheduleSlot timeSlot) {
        for (ScheduleSlot curr : timeSlots) {
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
    public List<ScheduleSlot> getWholeSchedule() {
        return timeSlots;
    }
}
