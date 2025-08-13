# Event Sourcing Demo Application

This application is a showcase for Ralf Westphal's ideas regarding event-based applications.

These ideas are in Ralf's Substack articles found below.  They are worth a read:

- [Killing the Entity: Event Sourcing done the Epistemic Way](https://ralfwestphal.substack.com/p/killing-the-entity)
- [AQ over CRUD](https://ralfwestphal.substack.com/p/aq-over-crud)
- [True Agility Requires Event Sourcing](https://ralfwestphal.substack.com/p/true-agility-requires-event-sourcing)

# Event Sourcing Defined

1. Events are the source of truth. Other repositories may be hydrated from the event stream, for search or reporting or other special requirements, but these other repositories can always be wiped and re-hydrated from the event stream, with no loss of data.
2. Events are immutable

# Demo Scope

First, a simple in-memory event store capable of the basic use cases outlined in the above links.  This provides a simple foundation.

Next, investigate each of the following concerns.

- Audit: how to list the events related to a person, within a datetime range, from a specific IP address, or regarding a specific business entity.
- Access control: 
    1. how to define access control lists on events
    2. how to restrict a list to only events the current user can see
    3. how to verify the current user is allowed to issue a particular command 
      - functional access: Whether the user is allowed to issue this command in the first place
      - data access: Whether the user can issue this command for this paritcular business entity
- Reporting: how to project the data for consumption by a traditional report engine
- Data migration: how to migrate data from an existing CRUD application
- Scale: explore tools for larger systems
    1. Many event types, many commands, many relationships between events
    2. Hundreds, thousands, millions, billions of events
- Queries: 
    1. How to populate lists
    2. How to show reports
- Event handling: 
    1. configurable set of commands to raise in response to events
    2. allow for predicates, so a command can be raised for events on a certain object, but not for events on other objects of the same type
- Extensibility
    1. Feature flags: enable or disable particular event handlers
    2. Customer-specific code
       - custom event handler
       - custom UI

# Documentation

Maintain a requirements document and architecture diagrams.

Requirements document in org-mode.

Architecture diagram in Mermaid C4.

# Technology Stack

- Programming language: Java since that's the language I know best.
- Events from server to browser: SSE
- Events from browser to server: REST

# Some tools that may be useful

- [Java Faker](https://github.com/DiUS/java-faker) Generate a stream of test events
- Report engines:
    1. [Apache Superset](https://superset.apache.org/)
    2. [Eclipse BIRT](https://projects.eclipse.org/projects/technology.birt)
- Event stores:
    1. [Apache Druid](https://druid.apache.org/)
    2. [Apache Cassandra](https://cassandra.apache.org/_/index.html)
    
Note, I am allergic to the Confluent stack.

    
