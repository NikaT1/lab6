package server.commands;

import server.IOForClient;
import server.collectionUtils.PriorityQueueStorage;
import sharedClasses.City;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.PriorityQueue;

/**
 * Класс для команды save, которая сохраняет в файл коллекцию.
 */

public class CommandSave extends Command {
    private static final long serialVersionUID = 147364832874L;
    /**
     * Поле, использующееся для временного хранения коллекции.
     */
    private transient final PriorityQueue<City> dop = new PriorityQueue<>(10, (c1, c2) -> (c2.getArea() - c1.getArea()));

    /**
     * Конструктор, присваивающий имя и дополнительную информацию о команде.
     */
    public CommandSave() {
        super("save", "сохранить коллекцию в файл", 0, false);
    }

    /**
     * Метод, исполняющий команду.
     *
     * @param ioForClient     объект, через который производится ввод/вывод.
     * @param commandsControl объект, содержащий объекты доступных команд.
     * @param priorityQueue   хранимая коллекция.
     */
    public byte[] doCommand(IOForClient ioForClient, CommandsControl commandsControl, PriorityQueueStorage priorityQueue) throws FileNotFoundException {

        PrintWriter printWriter = new PrintWriter(priorityQueue.getFilePath());
        printWriter.write("id,name,x,y,creationDate,area,population,metersAboveSeaLevel,establishmentDate,agglomeration,climate,age" + "\n");
        while (!priorityQueue.getCollection().isEmpty()) {
            City city = priorityQueue.pollFromQueue();
            printWriter.write(city.getId() + ",");
            printWriter.write(city.getName() + ",");
            printWriter.write(city.getCoordinates().getX() + ",");
            printWriter.write(city.getCoordinates().getY() + ",");
            printWriter.write(city.getCreationDate() + ",");
            printWriter.write(city.getArea() + ",");
            printWriter.write(city.getPopulation() + ",");
            if (city.getMetersAboveSeaLevel() == null) printWriter.write(",");
            else printWriter.write(city.getMetersAboveSeaLevel() + ",");
            if (city.getEstablishmentDate() == null) printWriter.write(",");
            else printWriter.write(city.getEstablishmentDate() + ",");
            if (city.getAgglomeration() == null) printWriter.write(",");
            else printWriter.write(city.getAgglomeration() + ",");
            printWriter.write(city.getClimate() + ",");
            if (city.getGovernor().getAge() == null) printWriter.write(",");
            else printWriter.write(city.getGovernor().getAge() + "");
            printWriter.write("\n");
            dop.add(city);
        }
        printWriter.flush();
        while (!dop.isEmpty()) {
            priorityQueue.addToCollection(dop.poll());
        }
        return null;
    }
}
