/**
 * Copyright (C) 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nordstrom.gatekeeper;

import com.google.protobuf.GeneratedMessageV3;
import java.io.Console;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class ClientCli {

  private final GatekeeperClient client;
  private final Console console;

  private ClientCli(GatekeeperClient client, Console console) {
    this.client = client;
    this.console = console;
  }

  public static void main(String[] args) {
    Console console = System.console();
    if (console == null) {
      System.err.println("no console");
      System.exit(1);
    }

    if (args.length != 2) {
      System.err.println("please specify:\n" + "<host> <port>");
      System.exit(1);
    }
    String host = args[0];
    int port = Integer.parseInt(args[1]);
    System.out.println(String.format("running gatekeeper client with host name: %s", host));
    System.out.println(String.format("running gatekeeper client with port: %d", port));
    GatekeeperClient client = new GatekeeperClient(host, port);
    ClientCli clientCli = new ClientCli(client, console);
    clientCli.readAction();
    System.exit(0);
  }

  private void readAction() {
    clearScreen(null);
    String action =
        console
            .readLine(
                "Please enter an action number: \n\n"
                    + "1 - authorize\n"
                    + "------------------------------\n"
                    + "2 - create subject permissions\n"
                    + "3 - add subject permissions\n"
                    + "4 - remove subject permissions\n"
                    + "5 - add subject roles\n"
                    + "6 - remove subject roles\n"
                    + "------------------------------\n"
                    + "7 - create role permissions\n"
                    + "8 - add role permissions\n"
                    + "9 - remove role permissions\n"
                    + "------------------------------\n"
                    + "10 - list subject authorizations\n"
                    + "11 - list role permissions\n"
                    + "------------------------------\n"
                    + "12 - quit\n\n")
            .trim();
    switch (action) {
      case "1":
        authorize();
        break;
      case "2":
        createSubjectPermissions();
        break;
      case "3":
        addSubjectPermissions();
        break;
      case "4":
        removeSubjectPermissions();
        break;
      case "5":
        addSubjectRoles();
        break;
      case "6":
        removeSubjectRoles();
        break;
      case "7":
        createRolePermissions();
        break;
      case "8":
        addRolePermissions();
        break;
      case "9":
        removeRolePermissions();
        break;
      case "10":
        listSubjectAuthZ();
        break;
      case "11":
        listRolePermissions();
        break;
      case "q":
      case "quit":
      case "bye":
      case "12":
        System.out.println("bye - have a nice life");
        System.exit(0);
        break;
      default:
        System.err.println(String.format("Unknown action %s", action));
        readAction();
        break;
    }
  }

  private void reReadAction() {
    console.readLine("Press enter to continue...");
    readAction();
  }

  private String getSubjectId() {
    String subjectId = console.readLine("Please enter the subject id: \n").trim();
    System.out.println(String.format("Subject id: %s", subjectId));
    return subjectId;
  }

  private String getRoleId() {
    String subjectId = console.readLine("Please enter the role id: \n").trim();
    System.out.println(String.format("Subject id: %s", subjectId));
    return subjectId;
  }

  private String[] getPermissions() {
    String input =
        console
            .readLine(
                "Please enter the requested, comma delimited permissions : \n"
                    + "e.g. read,write\n")
            .trim();
    String[] permissions =
        Stream.of(input.split(",")).filter(it -> !it.isEmpty()).toArray(String[]::new);
    System.out.println(String.format("Permissions: %s", input));
    return permissions;
  }

  private String[] getRoles() {
    String input =
        console
            .readLine(
                "Please enter the requested, comma delimited roles : \n" + "e.g. admin,guest\n")
            .trim();
    String[] roles = Stream.of(input.split(",")).filter(it -> !it.isEmpty()).toArray(String[]::new);
    System.out.println(String.format("Roles: %s", input));
    return roles;
  }

  private void authorize() {
    clearScreen(
        "**********************************\n"
            + "*************authorize************\n"
            + "**********************************");
    try {
      displayMessage(client.authorize(getSubjectId(), getPermissions()));
    } catch (ExecutionException | InterruptedException e) {
      displaySubjectModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void createSubjectPermissions() {
    clearScreen(
        "**********************************\n"
            + "****create subject permissions****\n"
            + "**********************************");
    try {
      displayMessage(
          client.createSubjectPermissions(
              getSubjectId(), Arrays.asList(getPermissions()), Arrays.asList(getRoles())));
    } catch (ExecutionException | InterruptedException e) {
      System.out.println(
          "WHOOPs, something went wrong - it's likely that the supplied subjectId *already* exists.\n"
              + "You should try to 'add subject permissions'");
      displayError(e);
    }
    reReadAction();
  }

  private void addSubjectPermissions() {
    clearScreen(
        "**********************************\n"
            + "******add subject permissions*****\n"
            + "**********************************");
    try {
      displayMessage(client.addSubjectPermissions(getSubjectId(), getPermissions()));
    } catch (ExecutionException | InterruptedException e) {
      displaySubjectModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void addSubjectRoles() {
    clearScreen(
        "**********************************\n"
            + "*********add subject roles********\n"
            + "**********************************");
    try {
      displayMessage(client.addSubjectRoles(getSubjectId(), getRoles()));
    } catch (ExecutionException | InterruptedException e) {
      displayRoleModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void removeSubjectPermissions() {
    clearScreen(
        "**********************************\n"
            + "****remove subject permissions****\n"
            + "**********************************");
    try {
      displayMessage(client.removeSubjectPermissions(getSubjectId(), getPermissions()));
    } catch (ExecutionException | InterruptedException e) {
      displaySubjectModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void removeSubjectRoles() {
    clearScreen(
        "**********************************\n"
            + "*******remove subject roles*******\n"
            + "**********************************");
    try {
      displayMessage(client.removeSubjectRoles(getSubjectId(), getRoles()));
    } catch (ExecutionException | InterruptedException e) {
      displaySubjectModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void createRolePermissions() {
    clearScreen(
        "**********************************\n"
            + "******create role permissions*****\n"
            + "**********************************");
    try {
      displayMessage(client.createRolePermissions(getRoleId(), getPermissions()));
    } catch (ExecutionException | InterruptedException e) {
      System.out.println(
          "WHOOPs, something went wrong - it's likely that the supplied roleId *already* exists.\n"
              + "You should try to 'add role permissions'");
      displayError(e);
    }
    reReadAction();
  }

  private void addRolePermissions() {
    clearScreen(
        "**********************************\n"
            + "*******add role permissions*******\n"
            + "**********************************");
    try {
      displayMessage(client.addRolePermissions(getRoleId(), getPermissions()));
    } catch (ExecutionException | InterruptedException e) {
      displayRoleModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void removeRolePermissions() {
    clearScreen(
        "**********************************\n"
            + "******remove role permissions*****\n"
            + "**********************************");
    try {
      displayMessage(client.removeRolePermissions(getRoleId(), getPermissions()));
    } catch (ExecutionException | InterruptedException e) {
      displayRoleModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void listSubjectAuthZ() {
    clearScreen(
        "**********************************\n"
            + "***list subject authorizations****\n"
            + "**********************************");
    try {
      displayMessage(client.listSubjectAuthZ(getSubjectId()));
    } catch (ExecutionException | InterruptedException e) {
      displaySubjectModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void listRolePermissions() {
    clearScreen(
        "**********************************\n"
            + "*******list role permissions******\n"
            + "**********************************");
    try {
      displayMessage(client.listRolePermissions(getRoleId()));
    } catch (ExecutionException | InterruptedException e) {
      displayRoleModifyErrorSuggestion();
      displayError(e);
    }
    reReadAction();
  }

  private void displaySubjectModifyErrorSuggestion() {
    System.out.println(
        "WHOOPs, something went wrong - it's likely that the supplied subjectId does not exist.");
  }

  private void displayRoleModifyErrorSuggestion() {
    System.out.println(
        "WHOOPs, something went wrong - it's likely that the supplied roleId does not exist.");
  }

  private void displayError(Exception e) {
    System.err.println(String.format("error: %s", e));
  }

  private void displayMessage(GeneratedMessageV3 messageV3) {
    System.out.println(String.format("Response: %s", messageV3));
  }

  public static void clearScreen(@Nullable String message) {
    System.out.print("\033[H\033[2J");
    System.out.flush();
    if (message != null) {
      System.out.println(message);
    }
  }
}
