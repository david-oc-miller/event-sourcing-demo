# Event Sourcing Learning Application

This application is for me to learn how to write an event-based application according to Ralf Westphal's ideas.

These ideas are in Ralf's Substack articles found below.  They are worth a read:

- [Killing the Entity: Event Sourcing done the Epistemic Way](https://ralfwestphal.substack.com/p/killing-the-entity)
- [AQ over CRUD](https://ralfwestphal.substack.com/p/aq-over-crud)
- [True Agility Requires Event Sourcing](https://ralfwestphal.substack.com/p/true-agility-requires-event-sourcing)

I am also using this repository as an ATDD Sandbox, for this class https://atdd-accelerator.optivem.com/, given by https://www.linkedin.com/in/valentinajemuovic.

# Event Sourcing Defined

1. Events are the source of truth.
2. Other repositories are generated from events and can always be recreated.  These secondary repositories serve special purposes (search, reporting, etc.).  They contain only data that is already in the event store.
3. Events are immutable.
  
# Documentation

GitHub Pages - https://david-oc-miller.github.io/event-sourcing-demo/

Project Boards - https://github.com/users/david-oc-miller/projects/1

  
[![pages-build-deployment](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/pages/pages-build-deployment/badge.svg)](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/pages/pages-build-deployment)

[![commit-stage-monolith](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/commit-stage-monolith.yml/badge.svg)](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/commit-stage-monolith.yml)
[![acceptance-stage](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/acceptance-stage.yml/badge.svg)](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/acceptance-stage.yml)
[![qa-stage](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/qa-stage.yml/badge.svg)](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/qa-stage.yml)
[![qa-signoff](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/qa-signoff.yml/badge.svg)](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/qa-signoff.yml)
[![prod-stage](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/prod-stage.yml/badge.svg)](https://github.com/david-oc-miller/event-sourcing-demo/actions/workflows/prod-stage.yml)
