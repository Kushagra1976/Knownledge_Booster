// Learning Objective:
// Design a type-safe Domain-Specific Language (DSL) in Kotlin for defining
// application configurations cleanly and expressively. We will use Kotlin's
// 'lambdas with receiver' feature to create a fluent, block-based syntax.
// This tutorial focuses on building nested configuration structures safely.

// --- 1. Define the Core Configuration Data Structures ---
// These are simple data classes that will hold our configuration values.
// They represent the "domain objects" that our DSL will help us build.
// Using `var` for properties allows us to modify them within the DSL blocks.

data class ServerConfig(
    var host: String = "localhost", // Default host for the server
    var port: Int = 8080,          // Default port
    var enabled: Boolean = true    // Default enabled status
) {
    // A helpful method to represent the configuration as a string for display.
    override fun toString(): String {
        return "Server(host='$host', port=$port, enabled=$enabled)"
    }
}

data class DatabaseConfig(
    var url: String = "jdbc:h2:mem:testdb", // Default H2 in-memory URL for the database
    var user: String = "sa",               // Default user
    var password: String = ""              // Default password
) {
    // A helpful method to represent the configuration as a string for display.
    // We mask the password for security reasons when printing.
    override fun toString(): String {
        return "Database(url='$url', user='$user', password='${"*".repeat(password.length)}')"
    }
}

// The top-level application configuration. It holds instances of our nested configs.
// By initializing `server` and `database` with default instances, we ensure that
// they are always available, even if their respective DSL blocks are not called.
data class ApplicationConfig(
    var name: String = "MyApplication", // Default application name
    var version: String = "1.0.0",      // Default version
    var server: ServerConfig = ServerConfig(), // Initialize with default server config
    var database: DatabaseConfig = DatabaseConfig() // Initialize with default database config
) {
    // A helpful method to represent the entire configuration as a string for display.
    override fun toString(): String {
        // Using `buildString` for efficient multi-line string construction.
        return buildString {
            append("Application(name='$name', version='$version') {\n")
            append("  $server\n") // Include server config's string representation
            append("  $database\n") // Include database config's string representation
            append("}")
        }
    }
}

// --- 2. Create the DSL Builder Functions ---
// These are the functions that form the "language" of our DSL.
// They heavily leverage Kotlin's 'lambda with receiver' to provide a natural,
// block-based syntax that feels like a custom language.

// This is the main entry point for our DSL.
// It takes a 'block' which is a lambda function.
// The key part: `ApplicationConfig.() -> Unit`
// WHAT: This specifies that the lambda `block` will be executed *on* an `ApplicationConfig` instance (the receiver).
// WHY: Inside this lambda, `this` will implicitly refer to the `ApplicationConfig` instance,
//      allowing us to directly access and modify its properties (like `name`, `version`)
//      or call its extension functions (like `server` or `database` which we'll define next)
//      without needing `config.name = "..."` or `this.name = "..."`.
fun application(block: ApplicationConfig.() -> Unit): ApplicationConfig {
    // 1. Create a new instance of ApplicationConfig to be configured.
    val config = ApplicationConfig()
    // 2. Execute the provided lambda block on this new config instance.
    //    The `apply` function is perfect for this: it calls the block on the receiver
    //    and then returns the receiver itself, making it fluent.
    config.apply(block)
    // 3. Return the fully configured ApplicationConfig instance.
    return config
}

// This is an 'extension function' for ApplicationConfig.
// WHAT: It means this function can be called directly on an `ApplicationConfig` instance,
//       as if it were a member function (e.g., `myConfig.server { ... }`).
// WHY: This allows us to write `server { ... }` directly inside the `application { ... }` block,
//      creating a nested, hierarchical structure for our configuration.
// The `ServerConfig.() -> Unit` lambda again indicates that inside the `server { ... }` block,
// `this` will refer to a `ServerConfig` instance, letting us configure it directly.
fun ApplicationConfig.server(block: ServerConfig.() -> Unit) {
    // 1. Create a new ServerConfig instance to be configured.
    val serverConfig = ServerConfig()
    // 2. Execute the provided lambda block on this new `serverConfig` instance.
    serverConfig.apply(block)
    // 3. Assign the fully configured `serverConfig` to the `server` property of the
    //    `ApplicationConfig` instance (which is `this` in the context of this extension function).
    this.server = serverConfig
}

// Similar to `server`, this is an extension function for ApplicationConfig to configure the database.
// WHAT: It provides a block to configure the `DatabaseConfig` part of the application.
// WHY: It maintains the consistent DSL syntax for nested configurations.
fun ApplicationConfig.database(block: DatabaseConfig.() -> Unit) {
    // 1. Create a new DatabaseConfig instance.
    val databaseConfig = DatabaseConfig()
    // 2. Execute the provided lambda block on this new `databaseConfig` instance.
    databaseConfig.apply(block)
    // 3. Assign the fully configured `databaseConfig` to the `database` property of the
    //    `ApplicationConfig` instance.
    this.database = databaseConfig
}

// --- 3. Example Usage of the DSL ---
fun main() {
    // Here's where we use our newly created DSL to define an application configuration.
    // Notice how clean and readable it is, almost like plain English or a configuration file.
    val myAppConfig = application {
        // Inside this block, 'this' refers to the ApplicationConfig instance.
        // We can directly set its properties:
        name = "MyAwesomeApp"
        version = "2.1.0"

        // We can call the 'server' extension function as if it were a regular method of ApplicationConfig.
        // Inside this 'server' block, 'this' refers to the ServerConfig instance.
        server {
            host = "prod.myapp.com" // Configure server host
            port = 443              // Configure server port (HTTPS default)
            enabled = true          // Explicitly set, even if it's the default
        }

        // Similarly for the 'database' configuration.
        // Inside this 'database' block, 'this' refers to the DatabaseConfig instance.
        database {
            url = "jdbc:postgresql://db.myapp.com:5432/production" // Configure database URL
            user = "app_user"                                     // Configure database user
            password = "secure_password_123"                      // Configure database password
        }
    }

    // Now, let's print the resulting configuration object to see what we've built.
    println("--- Generated Application Configuration ---")
    println(myAppConfig)

    println("\n--- Accessing individual configuration parts ---")
    println("App Name: ${myAppConfig.name}")
    println("Server Port: ${myAppConfig.server.port}")
    println("Database User: ${myAppConfig.database.user}")

    // Example of another config using some default values for brevity.
    println("\n--- Another Configuration (using some defaults) ---")
    val defaultAppConfig = application {
        name = "DefaultService"
        // Version will use default "1.0.0" (not explicitly set here)
        // Server will use all its defaults (localhost:8080) because the server {} block is omitted.
        // Database will use all its defaults (H2 in-memory) because the database {} block is omitted.
    }
    println(defaultAppConfig)
}