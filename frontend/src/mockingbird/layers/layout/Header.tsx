import React, { useState, useEffect } from 'react';
import { connect } from '@tramvai/state';
import { Container, Space, Title } from '@mantine/core';
import { getJson } from 'src/infrastructure/request';
import { Shadow } from './Shadow';

type Props = {
  assetsPrefix: string;
};

function Header({ assetsPrefix }: Props) {
  const [version, setVersion] = useState(null);
  useEffect(() => {
    getJson(`${assetsPrefix}version.json`)
      .then((res) => {
        if (res && res.version) setVersion(res.version);
      })
      .catch(() => null);
  }, [assetsPrefix]);
  const title = version ? `Mockingbird v${version}` : 'Mockingbird';
  return (
    <Shadow>
      <Container>
        <Space h="sm" />
        <Title order={2}>{title}</Title>
        <Space h="sm" />
      </Container>
    </Shadow>
  );
}

const mapProps = ({ environment: { ASSETS_PREFIX: assetsPrefix } }) => ({
  assetsPrefix,
});

export default connect([], mapProps)(Header);
