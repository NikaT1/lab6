package client;

import sharedClasses.Serialization;
import server.commands.Command;
import server.commands.CommandsControl;
import sharedClasses.UserInput;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class Client {
    private Selector selector;
    private DatagramChannel datagramChannel;
    private SocketAddress socketAddress;
    private final Serialization serialization;
    private final CommandsControl commandsControl;
    private final UserInput userInput;
    private final InputAndOutput inputAndOutput;
    private final Scanner scanner;

    public Client() {
        scanner = new Scanner(System.in);
        inputAndOutput = new InputAndOutput(scanner, true);
        userInput = new UserInput(inputAndOutput);
        serialization = new Serialization();
        commandsControl = new CommandsControl();
    }

    public InputAndOutput getInputAndOutput() {
        return inputAndOutput;
    }

    private void initialize() throws IOException {
        selector = Selector.open();
        datagramChannel = DatagramChannel.open();
        datagramChannel.configureBlocking(false);
    }

    public static void main(String[] args) {
        Client client = new Client();
        boolean flag = true;
        client.inputAndOutput.output("Введите порт:");
        int port = 0;
        while (true) {
            try {
                if (client.inputAndOutput.getScanner().hasNextLine()) port = Integer.parseInt(client.inputAndOutput.getScanner().nextLine());
                else System.exit(1);
                if (port < 0 || port >65535) {
                    client.inputAndOutput.output("Неверный формат порта, повторите ввод:");
                    continue;
                }
                break;
            } catch (IllegalArgumentException e) {
                client.inputAndOutput.output("Неверный формат порта, повторите ввод:");
            }
        }
        while (flag) {
            try {
                client.initialize();
                client.connect("localhost", port);
                client.getInputAndOutput().output("Введите команду:");
                flag = false;
                client.run();
            } catch (IOException e) {
                client.getInputAndOutput().output("Соединение не установлено");
                flag = client.getInputAndOutput().readAnswer("Повторить попытку? (yes/no)");
            }
        }
    }

    private void connect(String host, int port) throws IOException {
        socketAddress = new InetSocketAddress(host, port);
        datagramChannel.connect(socketAddress);
    }

    private void sendCommand(String[] s) throws IOException, ParseException {
        Command currentCommand;
        if (s.length > 0 && commandsControl.getCommands().containsKey(s[0])) {
            if (s[0].equals("save")) {
                inputAndOutput.output("Данная команда недоступна, повторите ввод");
                datagramChannel.register(selector, SelectionKey.OP_WRITE);
            } else {
                currentCommand = commandsControl.getCommands().get(s[0]);
                if (currentCommand.getAmountOfArguments() > 0) {
                    currentCommand.setArgument(s[1]);
                }
                if (currentCommand.isNeedCity()) {
                    currentCommand.setCity(userInput.readCity());
                }
                byte[] ser = Serialization.serializeData(currentCommand);
                if (ser != null) {
                    ByteBuffer buffer = ByteBuffer.wrap(ser);
                    datagramChannel.send(buffer, socketAddress);
                }
            }
        } else {
            inputAndOutput.output("Данной команды не существет, повторите ввод");
            datagramChannel.register(selector, SelectionKey.OP_WRITE);
        }
    }

    private void outputAnswer() throws IOException {
        byte[] bytes = new byte[100000];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        socketAddress = datagramChannel.receive(buffer);
        bytes = buffer.array();
        String outputForUser = (String) serialization.deserializeData(bytes);
        inputAndOutput.output(outputForUser);
        if (outputForUser != null && outputForUser.equals("Коллекция сохранена в файл, работа приложения завершается")) {
            System.exit(1);
        }
        if (outputForUser != null && outputForUser.equals("Возникла ошибка при сохранении коллекции")) {
            if (inputAndOutput.readAnswer("Хотите выйти без сохранения коллекции?")) System.exit(1);
            else inputAndOutput.output("Выход не выполнен");
        } else {
            if (outputForUser == null) {
                inputAndOutput.output("Ошибка сериализации команды; команда не выполнена");
                datagramChannel.register(selector, SelectionKey.OP_WRITE);
            }
        }
    }

    private void run() throws IOException {
        datagramChannel.register(selector, SelectionKey.OP_WRITE);
        while (true) {
            if (selector.select() == 0) {
                System.exit(0);
            }
            Set selectedKeys = selector.selectedKeys();
            Iterator keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = (SelectionKey) keyIterator.next();
                if (key.isReadable()) {
                    datagramChannel.register(selector, SelectionKey.OP_WRITE);
                    outputAnswer();
                }
                if (key.isWritable()) {
                    datagramChannel.register(selector, SelectionKey.OP_READ);
                    String[] s = null;
                    if (scanner.hasNextLine()) s = scanner.nextLine().split(" ");
                    else System.exit(1);
                    try {
                        sendCommand(s);
                    } catch (IndexOutOfBoundsException e) {
                        inputAndOutput.output("Введены не все аргументы команды");
                        datagramChannel.register(selector, SelectionKey.OP_WRITE);
                    } catch (Exception e) {
                        inputAndOutput.output("Произошла непредвиденная ошибка");
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }
                keyIterator.remove();
            }
        }
    }
}
