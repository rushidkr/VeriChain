import os
from typing import Any

import httpx

VERICHAIN_API_BASE_URL = os.environ.get("VERICHAIN_API_BASE_URL", "http://localhost:8080")


async def verify_credential(credential_id: str) -> dict[str, Any]:
    """Calls the Spring Boot backend's public verification endpoint. No auth needed -
    that endpoint is deliberately open to anyone, which is the whole point of the system."""
    url = f"{VERICHAIN_API_BASE_URL}/api/verify/{credential_id}"
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(url)
        response.raise_for_status()
        return response.json()
