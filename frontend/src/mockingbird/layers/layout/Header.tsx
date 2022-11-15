import React, { useState, useEffect } from 'react';
import { connect } from '@tramvai/state';
import { Shadow, Container, Row } from '@platform-ui/navigation';
import Text from '@platform-ui/text';
import { getJson } from 'src/infrastructure/request';

interface Props {
  assetsPrefix: string;
}

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
        <Row left={<Text size={19}>{title}</Text>} />
      </Container>
    </Shadow>
  );
}

const mapProps = ({ environment: { ASSETS_PREFIX: assetsPrefix } }) => ({
  assetsPrefix,
});

export default connect([], mapProps)(Header);
