package it.azraelsec;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Subparsers;

public class Client 
{
    public static void main( String[] args )
    {
        ArgumentParser argpars = ArgumentParsers.newFor("TURING Client")
            .build()
            .defaultHelp(true)
            .description("TURING distributed program client");
        Subparsers commandParser = argpars.addSubparsers().title("command")
            .help("Additional help").description("Valid commands").metavar("COMMAND");
        commandParser.addParser("register").help("registra l'utente");
        commandParser.addParser("login").help("effettua il login");
        commandParser.addParser("logout").help("effettua il logout");
        commandParser.addParser("create").help("");
        // Continue with the other client commands...
    }
}
