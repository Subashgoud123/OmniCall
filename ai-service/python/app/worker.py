from app.agents.livekit_agent import server
from livekit.agents import cli

if __name__ == "__main__":
    cli.run_app(server)
