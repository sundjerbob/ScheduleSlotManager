package raf.sk_schedule;

import raf.sk_schedule.api.ScheduleManager;
import raf.sk_schedule.exception.ScheduleException;
import raf.sk_schedule.filter.SearchCriteria;
import raf.sk_schedule.model.RoomProperties;
import raf.sk_schedule.model.ScheduleSlot;
import raf.sk_schedule.util.importer.RoomPropertiesImporterCSV;
import raf.sk_schedule.util.importer.ScheduleSlotImporterCSV;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    public void initialize(String startingDate, String endDate) {
        try {
            this.startingDate = new SimpleDateFormat(ScheduleSlot.dateFormat).parse(startingDate);
            this.endingDate = new SimpleDateFormat(ScheduleSlot.dateFormat).parse(endDate);

        } catch (ParseException e) {
            throw new ScheduleException("Start or end date is in wrong format!");
        }

    }

    public int loadRoomsSCV(String csvPath) throws IOException {
        rooms = RoomPropertiesImporterCSV.importRoomsCSV(csvPath);
        return rooms.size();
    }

    public int loadScheduleSCV(String csvPath) throws IOException {
        if (rooms.isEmpty())
            throw new ScheduleException("Your room properties are currently empty. You need to import them first in order to bind the scheduled slots with their location.");

        try {
            mySchedule = ScheduleSlotImporterCSV.importRoomsCSV(csvPath, rooms);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        System.out.println("start " + startingDate.getTime() + "  end: " + endingDate.getTime() + "loaded rows:  " + mySchedule.size());
        mySchedule.removeIf(slot -> ((slot.getStart().getTime() < startingDate.getTime()) || (slot.getStart().getTime() >= endingDate.getTime())));
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
                                + curr.getStartAsString().split(" +")[1]
                                + " and " + curr.getEndAsString().split("\\s")[1]
                                + " on: " + curr.getStartAsString().split(" +")[0]);
        }
        return mySchedule.add(timeSlot);
    }

    @Override
    public boolean scheduleRepetitiveTimeSlot(String s, String s1, long l, String s2) {
        return false;
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
            List<?> list = mySchedule.stream().filter(curr -> curr.getStart().getTime() < 9).toList();
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
    public List<ScheduleSlot> getFreeTimeSlots(String startDate, String endDate) {
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
    public int exportScheduleCSV(String filePath, String firstDate, String lastDate) {
        return 1;
    }

    @Override
    public int exportScheduleJSON(String filePath, String firstDate, String lastDate) {
        return 1;

    }

    @Override
    public int exportFilteredScheduleCSV(String filePath, SearchCriteria searchCriteria, String date, String date1) {
        return 1;
    }

    @Override
    public int exportFilteredScheduleJSON(String filePath, SearchCriteria searchCriteria, String date, String date1) {
        return 1;

    }

    @Override
    public List<ScheduleSlot> getSchedule(String date, String date1) {
        return null;
    }

    @Override
    public List<ScheduleSlot> getWholeSchedule() {
        return mySchedule;
    }

    private static File createFileIfNotExists(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            // Get the file name from the path
            String fileName = file.getName();

            try {
                // Create a new file in the same directory with the extracted name
                File newFile = new File(file.getParent(), fileName);

                if (newFile.createNewFile()) {
                    System.out.println("File created: " + newFile.getAbsolutePath());
                    return newFile; // Return the newly created File object
                } else {
                    System.err.println("Failed to create the file.");
                }
            } catch (IOException e) {
                System.err.println("An error occurred: " + e.getMessage());
            }
        }

        return file; // If the file already exists, return the existing File object
    }

    private static void writeStringToFile(File file, String content) {
        try {
            FileWriter fileWriter = new FileWriter(file, true); // 'true' enables append mode
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.write(content);
            writer.newLine(); // Add a newline character after the content
            writer.close();
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }
}
