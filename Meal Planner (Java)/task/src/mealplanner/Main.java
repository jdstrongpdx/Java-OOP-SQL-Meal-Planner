package mealplanner;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.sql.*;

/**
 * Meal Planner Program Summary
 * Purpose: This program builds a weekly meal plan via: validating and saving meals with ingredients to separate SQL
 * tables, creating a weekly meal plan in a third table, and using a query to sum the ingredients needed for the week,
 * exporting them to a text file.
 * 
 * Design:  This program has no public facing interfaces with the outputs being printing lines to the terminal or
 * exporting a file - so all members and classes are private except for main.  Maintains local synchronization of
 * stored Meal class objects with a SQL (Postgres) database - loading all SQL data into class objects on startup.
 * Will create new database schema if the database is not present on startup. Both statements and prepared statements
 * were used for practice - statements for table generation with no inputs and single run cycles.  Prepared statements
 * were used in combination with user input to prevent SQL injection and for efficiency.
 *
 * Personal: This is my second program created in Java while independently learning the language with an
 * intent to learn SQL, Classes, Methods, Access Modifiers, Checked Exceptions, File Handling and Javadocs.
 * Creation of this program required installing and running a local Postgres server to the required specifications
 * of the Hyperskill brief. Used pgAdmin4 to manage user roles and troubleshoot the JDBC statements for table creation,
 * insertion, and queries.
 */



public class Main {
  private final static Scanner scanner = new Scanner(System.in); // Do not change this line
  private int Mealid = 0;  // serial counter for the Mealid
  private int Ingredientid = 0;   // serial counter for the Ingredientid
  private int Planid = 0;  // serial counter for the Planid
  private boolean PlanCreated = false;
  private static Connection con = null;  // holds the SQL connection for use across methods
  private ArrayList<Meal> Meals = new ArrayList<>();  // stores Meal objects

  // class meal for getting and storing meal information
  class Meal {
    private String category;
    private String name;
    private String[] ingredients;

    /**
     * Class constructor that creates Meal objects from SQL data when the program starts
     * @param category    a String matching "breakfast", "lunch", or "dinner"
     * @param name        a String of the given name of the meal
     * @param ingredients a String list of ingredients used to make each meal
     */
    private Meal(String category, String name, String[] ingredients) {
      this.category = category;
      this.name = name;
      this.ingredients = ingredients;
      Mealid++;
      Ingredientid += ingredients.length;
    }

    /**
     * Class constructor that allows the user to add new meals via setter methods that validate text entries.
     * @throws SQLException  displays the stack trace of the error
     */
    private Meal() throws SQLException {
      try {
        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        this.category = setCategory();
        System.out.println("Input the meal's name:");
        this.name = setName();
        System.out.println("Input the ingredients:");
        this.ingredients = setIngredients();
        this.addToDatabase();
      } catch (SQLException e) {
        System.out.println("A SQL error has occurred during Meal creation.");
      }

      System.out.println("The meal has been added!");
    }

    /**
     * Method that uses PreparedStatements for insertion of data into the meals in ingredients tables.
     * @throws SQLException displays the stack trace of the error
     */
    private void addToDatabase() throws SQLException {
      try {
        // save meal data to the meal table
        String mealInsert = "INSERT INTO meals (meal_id, category, meal) VALUES (?, ?, ?)";
        try (PreparedStatement mealPreparedStatement = con.prepareStatement(mealInsert)) {
          mealPreparedStatement.setInt(1, Mealid);
          mealPreparedStatement.setString(2, this.category);
          mealPreparedStatement.setString(3, this.name);
          mealPreparedStatement.execute();
        }

        // save meal data to the ingredients table
        String ingredientInsert = "INSERT INTO ingredients (ingredient_id, ingredient, meal_id) VALUES (?, ?, ?)";
        try (PreparedStatement ingredientsPreparedStatement = con.prepareStatement(ingredientInsert)) {
          for (String ingredient : this.ingredients) {
            ingredientsPreparedStatement.setInt(1, Ingredientid);
            ingredientsPreparedStatement.setString(2, ingredient);
            ingredientsPreparedStatement.setInt(3, Mealid);
            ingredientsPreparedStatement.execute();
            Ingredientid++;
          }
        }
        Mealid++;
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    /**
     * Setter method for Meal category that validates user entry to selected Strings
     * @return  a String matching "breakfast", "lunch", or "dinner"
     */
    private static String setCategory() {
      List<String> choices = List.of("breakfast", "lunch", "dinner");
      while (true) {
        String option = scanner.nextLine();
        if (choices.contains(option)) {
          return option;
        } else {
          System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
        }
      }
    }

    /**
     * Setter method for Meal name that restricts entry to Alpha characters and spaces using REGEX
     * @return  a String of the given name of the meal
     */
    private static String setName() {
      while (true) {
        String option = scanner.nextLine();
        String test = option;
        if (Pattern.matches("[a-zA-Z\\s]+", option)) {
          test = test.trim();
          if (!test.isEmpty()) {
            return option;
          }
        }
        System.out.println("Wrong format. Use letters only!");
      }
    }

    /**
     * Setter method for Meal ingredients that accepts a string of Alpha characters, commas, and spaces using REGEX
     * - parsing single string by commas into a String list, stripping whitespaces.
     * @return  a String list of ingredients used to make each meal
     */
    private static String[] setIngredients() {
      while (true) {
        String splitable = scanner.nextLine();
        if (Pattern.matches("[a-zA-Z,\\s]+", splitable)) {
          String[] splitted = splitable.split(",");
          boolean check = false;
          for (int i = 0; i < splitted.length; i++) {
            splitted[i] = splitted[i].trim();
            if (splitted[i].isEmpty()) {
              if (!check) {
                check = true;
              }
            }
          }
          if (!check) {
            return splitted;
          }
        }
        System.out.println("Wrong format. Use letters only!");
      }
    }

    /**
     * Prints the class data of self
     */
    private void printSelf() {
      System.out.println("\nCategory: " + this.category);
      System.out.println("Name: " + this.name);
      System.out.println("Ingredients:");
      for (String ingredient : this.ingredients) {
        System.out.println(ingredient);
      }
    }
  }

  /**
   * Main method for running the program.  Will create a database connection and instantiate a member of self.  If
   * database tables are present, it will load all data into local memory as Meal Class objects, else it will create
   * empty database tables.  Will run a menu allowing user to interact with the program.
   * @param args            no command line arguments are used for this program
   * @throws SQLException   displays the stack trace of the error
   */
  public static void main(String[] args) throws SQLException {
    try {
      // create database and connections
      String DB_URL = "jdbc:postgresql:meals_db";
      String USER = "postgres";
      String PASS = "1111";

      Main.con = DriverManager.getConnection(DB_URL, USER, PASS);
      con.setAutoCommit(true);

      // instantiate main loading file data into the class and databases
      Main planner = new Main();

      // try to query the database - if found: load, else create tables.
      try {
        Statement testStatement = con.createStatement();
        ResultSet testSet = testStatement.executeQuery("SELECT * FROM meals");
        planner.loadDatabase();
      } catch (SQLException e) {
        planner.createDatabase();
      }

      // run the menu method
      menu(planner);

      // close the database connection and clear class attribute before exiting
      con.close();
      con = null;
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Creates new database tables for synchronizing Meal class data and the SQL database
   * @throws SQLException  displays the stack trace of the error
   */
  private void createDatabase() throws SQLException {
    try {
      Statement mealsStatement = con.createStatement();

      // drop the existing tables if they exist
      mealsStatement.executeUpdate("DROP TABLE IF EXISTS meals");
      mealsStatement.executeUpdate("DROP TABLE IF EXISTS ingredients");

      // create the meals table
      mealsStatement.executeUpdate("CREATE TABLE meals (" +
              "meal_id INTEGER PRIMARY KEY," +
              "category VARCHAR(30)," +
              "meal VARCHAR(30)" +
              ")");
      // create the ingredient table
      mealsStatement.executeUpdate("CREATE TABLE ingredients (" +
              "ingredient_id INTEGER PRIMARY KEY," +
              "ingredient VARCHAR(30)," +
              "meal_id INTEGER" +
              ")");

      mealsStatement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Loads all database data into Meal class data at the start of the program and stores all Meal objects in the 
   * "meals" ArrayList.  This list is not used elsewhere, but was done to learn loading/synchronization between
   * local class objects and SQL database storage.
   * @throws SQLException   displays the stack trace of the error
   */
  private void loadDatabase() throws SQLException {
    try {
      // load meal data from the database
      Statement mealStatement = con.createStatement();
      ResultSet mealSet = mealStatement.executeQuery("SELECT * FROM meals");
      if (mealSet.next()) {
      do {
          String category = mealSet.getString("category");
          String name = mealSet.getString("meal");
          int mealid = mealSet.getInt("meal_id");
          // load ingredient data from the database
          Statement ingredientStatement = con.createStatement();
          ResultSet ingredientSet = ingredientStatement.executeQuery("SELECT * FROM ingredients WHERE meal_id = " + mealid);
          String[] ingredients = new String[10];
          int count = 0;
          while (ingredientSet.next()) {
            String ingredient = ingredientSet.getString("ingredient");
            if (!ingredient.isEmpty()) {
              ingredients[count] = ingredient;
              count++;
            }
          }
          String[] cleaned = new String[count];
          for (int j = 0; j < count; j++) {
            cleaned[j] = ingredients[j];
          }
          ingredientStatement.close();
          // save the meal and ingredient data to a Meal object
        Meals.add(new Meal(category, name, cleaned));
        } while (!mealSet.isClosed() && mealSet.next()); }
      mealStatement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }


  /**
   * The main menu that the user interacts with validating their text entries match to a named method.
   * @param planner         an instantiation of Main for working with class data
   * @throws SQLException   displays the stack trace of the error
   */
  private static void menu(Main planner) throws SQLException {
    try {
      while (true) {
        System.out.println("What would you like to do (add, show, plan, save, exit)?");
        String option = scanner.nextLine();
        switch (option) {
          case "add" -> planner.getMeal();
          case "show" -> planner.printNames();
          case "plan" -> planner.planWeek();
          case "print" -> planner.printWeek();
          case "save" -> planner.save();
          case "exit" -> {
            System.out.println("Bye!");
            System.exit(0);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Method for adding new Meals into the program - utilizing Meal class for validating user input and saving all
   * necessary information
   * @throws SQLException   displays the stack trace of the error
   */
  private void getMeal() throws SQLException {
    try {
      Meals.add(new Meal());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Generates a weekly meal plan.  For each day and for each category, the meals matching the category are found
   * and presented to the user - which they must make a valid selection of.  After selecting all 21 meals, the weekly
   * plan is printed out (using a helper method).  The results of the weekly plan are saved in a new SQL table using
   * relational properties for all searches and saves.  Will toggle Main attribute PlanCreated to true upon
   * completion for later use in save method.
   */
  private void planWeek() throws SQLException {
    // drop and create the plans table
    Statement planStatement = con.createStatement();
    planStatement.executeUpdate("DROP TABLE IF EXISTS plan");
    planStatement.executeUpdate("CREATE TABLE plan (" +
            "plan_id INTEGER PRIMARY KEY," +
            "day VARCHAR(10)," +
            "category VARCHAR(30)," +
            "meal_id INTEGER" +
            ")");

    planStatement.close();

    String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    String[] categories = {"breakfast", "lunch", "dinner"};
    try {
      // for each day of the week
      for (String day : days) {
        System.out.println(day);
        // for each meal of the day
        for (String category : categories) {
          // query list of available meals
          String find = "SELECT * FROM meals WHERE category = ? ORDER BY meal ASC";
          try (PreparedStatement getMeals = con.prepareStatement(find)) {
            getMeals.setString(1, category);
            ResultSet mealSet = getMeals.executeQuery();
            if (!mealSet.next()) {
              System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
              getMeals.close();
              return;
            }
            do {
              String name = mealSet.getString("meal");
              System.out.println(name);
            } while (mealSet.next());
          }
          // get and validate user entry on meal choice
          System.out.println("Choose the " + category + " for " + day + " from the list above:");
          boolean flag = false;
          int mealInt = -1;
          while (!flag) {
            String option = scanner.nextLine();
            find = "SELECT * FROM meals WHERE category = ? AND meal = ?";
            try (PreparedStatement findMeals = con.prepareStatement(find)) {
              findMeals.setString(1, category);
              findMeals.setString(2, option);
              ResultSet findSet = findMeals.executeQuery();
              if (!findSet.next()) {
                System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
              } else {
                mealInt = findSet.getInt("meal_id");
                flag = true;
              }
            }
          }
          // add valid meal choice entry to the database
          String plan = "INSERT INTO plan (plan_id, day, category, meal_id) VALUES (?, ?, ?, ?)";
          try (PreparedStatement planInsert = con.prepareStatement(plan)) {
            planInsert.setInt(1, Planid);
            planInsert.setString(2, day);
            planInsert.setString(3, category);
            planInsert.setInt(4, mealInt);
            planInsert.execute();
            Planid++;
          }
        }
        System.out.println("Yeah! We planned the meals for " + day + ".");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    PlanCreated = true;
    printWeek();
  }

  /**
   * Prints a weekly meal list to the screen.  For each day and for each category, queries the plan table and prints
   * the first found meal.
   * @throws SQLException  displays the stack trace of the error
   */
    private void printWeek() throws SQLException {
      String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
      String[] categories = {"breakfast", "lunch", "dinner"};
      // for each day of the week
      for (String day : days) {
          System.out.println("\n" + day);
        // for each meal of the day
        for (String category : categories) {
          // query list of available meals
          String find = "SELECT * FROM plan WHERE day = ? AND category = ?";
          try (PreparedStatement getPlan = con.prepareStatement(find)) {
            getPlan.setString(1, day);
            getPlan.setString(2, category);
            ResultSet mealSet = getPlan.executeQuery();
            if (!mealSet.next()) {
              getPlan.close();
            } else {
              int mealID = mealSet.getInt("meal_id");
              find = "SELECT * FROM meals WHERE meal_id = ?";
              try (PreparedStatement getMeal = con.prepareStatement(find)) {
                getMeal.setInt(1, mealID);
                ResultSet foundMeal = getMeal.executeQuery();
                if (foundMeal.next()) {
                  String mealName = foundMeal.getString("meal");
                    System.out.println(category + ": " + mealName);
                  }
                }
              }
              mealSet.close();
            } catch (SQLException e) {
            e.printStackTrace();
            }
          }
        }
      PlanCreated = true;
    }

  /**
   * Runs a SQL query to final the count of all meals and the count of all ingredients in said meals.  Uses the
   * results to generate text output that is appended to a file in the local path.
   * @throws SQLException
   */
  private void save() throws SQLException {
    String find = "SELECT ingredient, count(ingredient_id) " +
            "FROM ingredients " +
            "JOIN meals ON meals.meal_id=ingredients.meal_id " +
            "JOIN plan ON meals.meal_id=plan.meal_id " +
            "GROUP BY ingredient;";
    try (PreparedStatement getList = con.prepareStatement(find)) {
      ResultSet mealSet = getList.executeQuery();
      System.out.println("\nInput a filename:");
      String filename = scanner.nextLine();
      File file = new File("./" + filename);
      try (FileWriter fileWriter = new FileWriter(file, true)) {
        while (mealSet.next()) {
          String ingredient = mealSet.getString("ingredient");
          int count = mealSet.getInt("count");
          String output = "";
          if (count > 1) {
            output = ingredient + " x" + count + "\n";
          } else {
            output = ingredient + "\n";
          }
          fileWriter.write(output);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch (SQLException e) {
      System.out.println("Unable to save. Plan your meals first.");
    }
    System.out.println("Saved!");
    }

  /**
   * Gets validated category input from user, queries database and prints records matching requested category.
   * @throws SQLException   displays the stack trace of the error
   */
  private void printNames() throws SQLException {
    // get category input from user (validated in method)
    System.out.println("\nWhich category do you want to print (breakfast, lunch, dinner)?");
    String category = Meal.setCategory();
    try {
      PreparedStatement mealStatement = con.prepareStatement("SELECT * FROM meals WHERE category = ?");
      mealStatement.setString(1, category);
      ResultSet mealSet = mealStatement.executeQuery();
      // if no meals were found in the database
      if (!mealSet.next()) {
        System.out.println("No meals found.");
        mealStatement.close();
        return;
      }
      // load meals data from the database
      System.out.println("Category: " + category);
      do {
        String name = mealSet.getString("meal");
        System.out.println("\nName: " + name);
        int meal_id = mealSet.getInt("meal_id");
        // load ingredient data from the database
        PreparedStatement ingredientStatement = con.prepareStatement("SELECT * FROM ingredients WHERE meal_id = ?");
        ingredientStatement.setInt(1, meal_id);
        ResultSet ingredientSet = ingredientStatement.executeQuery();
        System.out.println("Ingredients:");
        while (ingredientSet.next()) {
          String ingredient = ingredientSet.getString("ingredient");
          System.out.println(ingredient);
        }
        ingredientStatement.close();
      } while (mealSet.next());
      System.out.println();

      mealStatement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
