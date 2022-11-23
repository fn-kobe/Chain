package command;

import java.util.ArrayList;
import java.util.List;

public class Command {
    CommandType commandType;
    List<String> arguments;

    public Command() {
        this.commandType = CommandType.EOther;
        this.arguments = new ArrayList<>();
    }

    public Command(CommandType commandType, List<String> arguments) {
        this.commandType = commandType;
        this.arguments = arguments;
    }

    public Command(CommandType commandType, String argument) {
        this.commandType = commandType;
        this.arguments = new ArrayList<>();
        this.arguments.add(argument);
    }
}
