package com.bluereligion.accountmerge;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;

import com.bluereligion.accountmerge.service.AccountsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A command-line entry point into the AccountService.  This entry point requires two arguments:
 *  - input filename
 *  - output filename
 *
 *  Fewer or more arguements with return an exception.
 */
@SpringBootApplication
public class AccountMerge
        implements CommandLineRunner {

    @Autowired
    private AccountsService accountsService;

    private static final Integer REQUIRED_NUM_OF_ARGS = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountMerge.class);

    public static void main(String[] args) {

        new SpringApplicationBuilder(AccountMerge.class)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .web(WebApplicationType.NONE)
                .properties("spring.config.name=application")
                .build()
                .run(args);

    }


    /**
     * Initiates the service after validating arguments.
     * @param args - input filename and output file name.
     * @throws Exception
     */
    public void run(String... args) throws Exception {

        if ( !areArgumentsValid(args) ) {
            LOGGER.debug("Commandline arguments are not valid="+ Arrays.toString(args));
            this.outputUsage();
            System.exit(1);
        }

        LOGGER.debug(String.format("Arguments received=%s", Arrays.toString(args)));
        accountsService.processAccounts(args[0], args[1]);
    }


    /**
     * Validates the arguments passed via the command line.
     * The A\arguments are valid if:
     *   - 2 and only 2 arguments have been passed.
     *   - The argument names are not equal.
     * @param arguments
     * @return
     */
    private boolean areArgumentsValid(String[] arguments) {

        if ( arguments.length == 0 ) {
            LOGGER.error("No arguments detected from command line.");
            this.outputUsage();
            System.exit(1);
        }

        if ( arguments.length != REQUIRED_NUM_OF_ARGS ) {
            LOGGER.error(String.format("Invalid number of arguments received. Expected=%s, Received=%s", REQUIRED_NUM_OF_ARGS, arguments.length));
            return false;
        }

        if ( arguments[0].equals(arguments[1]) ) {
            LOGGER.error("The arguments entered can not be the same.");
            return false;
        }

        return true;
    }


    /**
     * Outputs usage guide to terminal.
     */
    private void outputUsage() {
        System.out.println("********************************************************************************");
        System.out.println("\n[Usage]");
        System.out.println("\taccount_merge <input_file> <output_file>");
        System.out.println("\n\tFor example:");
        System.out.println("\n\t\taccount_merge data/input.csv output.csv");
        System.out.println("\nNote: The input.csv output.csv file names need to be different\n\n");
        System.out.println("********************************************************************************");
    }

}
