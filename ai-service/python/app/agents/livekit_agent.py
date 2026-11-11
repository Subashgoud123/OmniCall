from livekit.agents import AgentServer, JobContext, cli

from app.agents.call_agent import run_call
from app.config.settings import settings

server = AgentServer()


@server.rtc_session(agent_name=settings.agent_name)
async def entrypoint(ctx: JobContext) -> None:
    await run_call(ctx)


if __name__ == "__main__":
    cli.run_app(server)
