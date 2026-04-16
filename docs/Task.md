# todo 
- read the text below
- make a good spec out of it, save it in docs/
- select technologies to be used and document it in docs/stack.md
- prepare the AI project environment based on this task, spec and technologies - generate needful set of skills, agents, instructions that later will be used by yourself
- generate an implementation plan
  - plan must contain phases. each phase should be short and simple, but still bring a value to the product
  - phases may contain sub steps
  - make a commit with valuable commit message after each phase.
- implement the plan, document completion of every step, so you can proceed from any safepoint in future.
- every change, decision or design should be documented


# idea
- it will be a coding-game.
- need to implement an Algo for a smart vacuum cleaner, implementing some interface.
- there will be a UI with "random" map where cleaner starts in random location and start to execute the coded algo
- every cleaned tile brings a point
- game is limited wth time (iterations), the final score is built up from cleaned tiles and iterations (lower - better)

# details
- map generation is random, but can also be generated using some hash in case we want to have some repetable competition.
  - user can provide "size" of the map - some integer number, that will somehow limit the size - amount of tiles. 
    may not be 1 to 1 mapping. can also be a preset of "tiny, small, medium..."
  - maps generated with the same hash should be identical
  - starting point is also a property of the map
- randomly generated maps stil should "look like" a room - it must be limited with "walls", may be of any form. may have obstacles inside - a tile of several surrounded by walls
  - maps form can be any, but we should try to generate something "expected" forms
  - every tile of the random map should be reachable
  - it should be clear how to "clean this map" for human observer
- we want leaderboards stored in some persistent place locally. this is optional and code should work without it as well. 
  - user may want to specify a path to a file with saved leaderboards and then program should use this file for storing results
  - after cleanup is completed, putting it to leaderboard is optional
  - leaderboard contains
    - robot avatar (user can select from suggestions before start)
    - username (faked movie hero if not provided)
    - map hash
    - iterations used and iterations available initially
    - score
    - "trace" of execution for playback
  - it should be possible to "repeat/playback" some saved clean, just by playing it (no delegations to algo)
  - it should be also possible to retry this attempt with different algo (or same but after modifications)
- tiles on the map are "little" squares. 
- code can contain many implemented algoritms , user can select one to run
- time is "virtual" here, in every iteration robot can do a move in each of 4 directions.
- to clean the tile robot should just move on it.
- UI animates robots movements, avoid "jumps"
- user can configure some params on start - they should be saved between the launches and page refreshes (cookies?)
 

# scenarios
- Basic
  - user opens site, setup the scene:
    - generates a random map or provides a hash
    - enters his name or use default or have option to generate next random
    - selects robot avatar or random will be used
    - selects amount of iterations or some default value is set. - this can be "progress bar selector with snapped values" - |---o-----|
  - user sees map on the screen and robot in starting position
  - user can select algo from list of available algos
  - user clicks RUN button and robot starts cleaning
  - user can pause, resume, speed up (less lag between iterations) and speed down the process
  - user can interrupt the clean or wait until robot finishes or out of iterations
  - user sees his score and can save it to leaderboard (if provided)

- Leaderboard
  - if user provides leaderboard file or system can find one in working dir user can see leaderboards
  - for every row in leaderboard user can re-attempt the same setup (same map, iterations) but individual username, algo and avatar
  - for every row in leaderboard user can "watch" the replay


# tech

- it should be a local java spring application. java 21, latest spring. Use Faker for random data generation 
- runnable with single Run command (one entry point)
- It should bring up the endpoint that exposes js web ui, the same endpoint (with different path) is used for UI to communicate with app
- UI is thin, everything is happening on server side
- There must be tests for "infra" code. 
- There should a java interface of robot algo and some model classes it can operate with 
  - The implementations of this interface should be registered (for example with spring-like annotation) and eventually be available in ui for selection
  - The interface for the robot algo should be well documented and "easy". it should basically contain one method: Direction next(Tile tile)
  - Tile may have some information - isClean, hasWall(Direction direction), etc. let's keep it minimal for now
  -  Every algo should pass some unit tests with predefined simple maps (square, corridor, circle, etc), given limited amount of iterations on this map. such test should allow developer to get through some happy paths and corner cases. test should not expect robot to completely cleanup the suggested map, but at least not fail with exceptions et.
  -  if next() call creates exceptions - this should be treated as finish (not successful, with zero points) no matter how far and how good it was.
  - there are no limitations in how complicated the algo code can be
  - there will be some samples of implementations - starting with random and some "stupid-always-to-the-left" algos
- leaderboard can be stored in some local file-database. no need in remote connections.
