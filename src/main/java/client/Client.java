package client;

import collection.InputAndOutput;
import collection.Serialization;
import server.commands.Command;
import server.commands.CommandsControl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
        while (flag) {
            try {
                client.initialize();
                client.connect("localhost", 666);
                client.getInputAndOutput().output("Соединение установлено");
                flag = false;
                client.run();
            } catch (IOException e) {
                client.getInputAndOutput().output("Соединение не установлено");
                flag = client.getInputAndOutput().readAnswer("Повторить попытку? (yes/no)");
            }
        }
    }

    private void connect(String host, int addr) throws IOException {
        socketAddress = new InetSocketAddress(host, addr);
        datagramChannel.connect(socketAddress);
    }

    private void run() throws IOException {
        datagramChannel.register(selector, SelectionKey.OP_WRITE);
        while (true) {
            int count = selector.select();
            if (count == 0) {
                System.exit(0);
            }
            Set selectedKeys = selector.selectedKeys();
            Iterator keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = (SelectionKey) keyIterator.next();
                if (key.isReadable()) {
                    byte[] bytes = new byte[100000];
                    datagramChannel.register(selector, SelectionKey.OP_WRITE);
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    socketAddress = datagramChannel.receive(buffer);
                    bytes = buffer.array();
                    String outputForUser = (String) serialization.deserializeData(bytes);
                    inputAndOutput.output(outputForUser);
                }
                if (key.isWritable()) {
                    datagramChannel.register(selector, SelectionKey.OP_READ);
                    String[] s = scanner.nextLine().split(" ");
                    Command currentCommand = commandsControl.getCommands().get(s[0]);
                    if (currentCommand.getAmountOfArguments() > 0) {
                        currentCommand.setArgument(s[1]);
                    }
                    if (currentCommand.isNeedCity()) {
                        currentCommand.setCity(userInput.readCity());
                    }
                    ByteBuffer buffer = ByteBuffer.wrap(serialization.serializeData(currentCommand));
                    datagramChannel.send(buffer, socketAddress);
                }
                keyIterator.remove();
            }
        }

    }
}
